# Load balancer [![Build Status](https://travis-ci.org/slamdev/load-balancer.svg?branch=master)](https://travis-ci.org/slamdev/load-balancer)

## Technologies stack
* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Gradle 2.14](https://docs.gradle.org/current/userguide/userguide.html) - project build system

## Development workflow
[GitHub Flow](https://guides.github.com/introduction/flow/) is used on this project:
* each task should be done in separate branch
* after task is developed, [pull request](https://help.github.com/articles/proposing-changes-to-a-project-with-pull-requests/) to **master** branch should be created and assigned to responsible for application part person
* after pull request is created, CI server will process static code checks (build project, validate code, run tests), if any check fails the pull request will be marked as failed and developer should and fix issues
* after all checks passed, code should be reviewed by pull request assignee, and if there are no remarks, he\she should merge it to master branch and remove the obsolete task branch

## Project setup
[IntelliJ IDEA Ultimate](https://www.jetbrains.com/idea/download/) is used for project development. The plugins below should be installed:
* Git Integration
* Github
* Gradle
* JavaScript Support
* JUnit
* Lombok Plugin

### Step-by-step
1. Open idea and select **Checkout from Version Control** -> **GitHub**
2. Set Git Repository URL to **git@github.com:slamdev/load-balancer.git**
3. Press **Clone**
4. In the **Import project** window select **Import project from the external project model** and press **Next**
5. In the **Import Project** window leave all default options and press **Finish**
6. In the **Gradle project data to import** leave all default options and press **OK**
7. If **Unregistered VCS root detected** warning appears, press **Add root**
8. Enable **annotation processing** in IDEA settings
9. Select **View** -> **Tool windows** -> **Gradle** menu item
10. In the appeared **Gradle projects** window select **Execute gradle task** icon (green circle)
11. In the appeared **Run Gradle Task** window type **clean build** to the **Command line** input and press **OK**
12. Wait until gradle download all dependencies and build the project. The first time it could take up to 5 minutes
