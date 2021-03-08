# Multi live copy

The project on AEM can create multiple live copies in one click taking inputs from CSV

## Description

The project overlays the sites admin console and adds option["Multi live copy"] on create button. 
The wizard on the process step takes a CSV file as input [title and name] for the live copies it is going to create.
Other options[exclude subpages,rollout config] are reused for all the created live copies.

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the
following command:

    mvn clean install -PautoInstallSinglePackage

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallSinglePackagePublish

Or alternatively

    mvn clean install -PautoInstallSinglePackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

Or to deploy only a single content package, run in the sub-module directory (i.e `ui.apps`)

    mvn clean install -PautoInstallPackage
