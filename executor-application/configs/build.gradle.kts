plugins {
    java
    id("template-processing")
}

description = "configs"

templateProcessing {

    template("destinationRule") {
        templatePath.set("templates/DestinationRuleTemplate.yaml")
        outputDir.set("generated-manifests/destination-rules")
        outputFilePattern.set("{name}DestinationRule.yaml")

        replacement {
            name = "serviceA"
            placeholder("from", "serviceAValue")
            placeholder("to", "serviceBValue")
        }

        replacement {
            name = "serviceB"
            placeholder("from", "serviceCValue")
            placeholder("to", "serviceDValue")
        }

    }

    // Configure VirtualService template processing
    template("virtualService") {
        templatePath.set("templates/VirtualServiceTemplate.yaml")
        outputDir.set("generated-manifests/virtual-services")
        outputFilePattern.set("{name}VirtualService.yaml")

        replacement {
            name = "serviceA"
            placeholder("from", "v1")
            placeholder("to", "v2")
        }

        replacement {
            name = "serviceB"
            placeholder("from", "canary")
            placeholder("to", "stable")
        }
    }
}

