#!/bin/bash

contains() {
    local item="$1"
    shift
    local array=("$@")

    for element in "${array[@]}"; do
        if [[ "$element" == "$item" ]]; then
            return 0
        fi
    done
    return 1
}

if ! git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    echo "This script must be run inside a git repository."
    exit 1
fi

current_branch=$(git rev-parse --abbrev-ref HEAD)

if ! [ "$current_branch" = "master" ]; then
    exit 0
fi

last_tag=$(
  git for-each-ref --format '%(refname:short)' refs/tags | grep '^D-' | sort -V | while read -r tag; do
      commit=$(git rev-list -n 1 "$tag")
      # Tag prediction on merge commit
      is_merge_commit=$(git log -1 --pretty=%P "$commit" | grep -q ' ' && echo true || echo false)
      # Tag prediction on master branch
      is_master_branch_tag=$(git branch --contains "$commit" | grep -q '\bmaster\b' && echo true || echo false)
      echo "$tag $is_merge_commit $is_master_branch_tag"
  done | grep "true true" | tail -n 2 | head -n 1 | cut -d' ' -f1
)

commits=$(git log --reverse "$last_tag..$current_branch" --pretty=format:"%s%n%H")
ticket_messages=()
tickets_in_increment=()

while IFS= read -r line; do

    commit_message="$line"
    ticket_identifiers=$(echo "$commit_message" | awk -F '[][]' '{print $2}')
    ticket_message=$(echo "$commit_message" | awk -F '[][]' '{print $4}')

    for ticket in $ticket_identifiers; do

        plain_ticket="${ticket//[^a-zA-Z0-9-]/}"

        if ! contains "$plain_ticket" "${tickets_in_increment[@]}"
        then
          tickets_in_increment+=("$plain_ticket")
          ticket_messages+=("$plain_ticket - $ticket_message (https://jira.sberbank.ru/browse/$plain_ticket)")
        fi

    done
done <<< "$commits"

if [ ${#ticket_messages[@]} -eq 0 ]; then
    echo "Инкремент относительно сборки $last_tag не найден."
else
    echo "Инкремент относительно сборки $last_tag:"
    for message in "${ticket_messages[@]}"; do
        echo "$message"
    done
fi
