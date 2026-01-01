# Development
This file contains notes about developing the project.

## Build profiles

The `io.github.tomboyo.lily.mode=dev` system property may be set to enable 
in-development features of the lily code generator. The maven `dev` profile 
does so and is active by default. In IDEs, it is necessary to add this 
property to test run configuration.

By default in end-user environments, this flag is set to `production`, which has
no special meaning other than to disable development features.

The dev mode flag's goal is to prevent users from seeing unfinished work, 
but does not need to completely disable in-progress code so long as that 
code has no obvious artifacts or impacts on the user experience.

The example project will build with development features enabled by default. 
To build the example project _without_ development features, run:
```sh
./mvnw -pl modules/example -P\!dev clean compile
```
