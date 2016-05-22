#  Semantic Actions: Phase One

For the full list of instructions for the semantic actions please refer to the
[CS 331](http://www.cs.vassar.edu/~cs331) course page.

Once again, the project uses TDD (Test Driven Development); the default 
project contains a suite of unit tests.  Your job is to get all the tests
to pass.  Feel free to write additional tests, in particular tests that
should be rejected by the parser.

## Project Setup

This assignment is slighty different from the others as the `Parser` is still 
the `main` program.  While most of the code you write will go in the semantic-actions
project the actions themselves are called by the parser.

### IntelliJ

If you don't have all your assignments in a single project, now might be a
a good time to create a single project, as follows: 

1. Create a new Empty Project in IntelliJ
1. For each previous assignment
  1. Select `File -> New -> Module from existing sources`
  1. Select the assignment directory
  1. Select `Import project from an external model`
  1. Select `Maven` and then Click `Next`
  1. Accept the remaining defaults. Be sure to select Java 8 as the JDK.

With all the assignments in a single project, IntelliJ is pretty good at
picking up code changes without having to run `mvn install`.

### Parser Changes

1. Add a dependency to your semantic-actions project

  ```xml
  <dependency>
      <groupId>edu.vassar.cs.cmpu331</groupId>    
      <artifactId>semantic-actions</artifactId>
      <version>1.0.0</version>
  </dependency>
  ```
  **NOTE** The above goes in the `pom.xml` file in your Parser project!
  
1. Handle the case where `stackTop.isAction()` is true to call the action's
`execute` method.

1. Create a `lookup` method that calls the lookup method in the semantic actions (in order to fetch things from the symbol table for debugging and testing purposes).

1. Move the `semanticActions/SemanticActionsTest` class from the semantic-actions
`src/test/resources` directory to the `src/test/java` directory in your parser project.

1. Move the files in `src/test/resoures/phase1` from the semantic-actions project
to your parser project.

1. Disable the `ParserTest` class by adding a `@Ignore` annotation just before
the class declaration.

**NOTE** If (when) you find and fix bugs in your other assignments you may
need to run `mvn install` again to install the fixed version into your
local repository.

## Continuous Integration with Travis-CI

If you have already set up [Travis-CI](https://education.travis-ci.com)
for the Parser assignment you simply need to run `mvn install:install-file`
in your LexicalAnalzyer, SymbolTable, and SemanticActions projects to 
install the jar files to your local mvn-repo directory. Then push your mvn-repo to
GitHub.

## Setting Up Travis

The project already comes with all the configuration files needed for 
[Travis-CI](https://education.travis-ci.com) to build your project whenever
code is pushed to GitHub.  The only wrinkle is that Travis will not have access
to your the other modules (lexical analyzer, symbol table, etc.). This is not a problem if you are using the model
solutions (not recommended), but if you want
to use your own modules and Travis at the same time you will need
to deploy your modules to a public Maven repository that Travis
can access.

Fortunately, GitHub makes it easy to host an ad-hoc public repository that
Travis can locate.

### Creating an Ad-Hoc Maven Repository.

1. Login to GitHub and create a public repository under your own user account.
  Name the repository `mvn-repo`.
1. Clone this repository to your computer.
  
  ```
  $> git clone https://github.com/your-name/mvn-repo.git
  ```
1. Install your all your projects into the mvn-repo directory you just cloned.
  
  ```
  $> cd [lexical-analyzer] 
  $> mvn clean package
  $> mvn install:install-file -DpomFile=pom.xml -Dfile=target/lexical-analyzer-1.0.0.jar -DlocalRepositoryPath=[repo]
  ```

  Where:

  1. `[lexical-analyzer]` is the directory containing your lexical analyzer project.
  1. `[repo]` is the directory containing the `mvn-repo` you cloned from GitHub.

  Repeat the above steps for your SymbolTable and SemanticActions modules as well.
  
  **NOTE** For Windows users the `-Dfile` path will be `target\lexical-analyzer-1.0.0.jar`.
1. Push your local `mvn-repo` to GitHub.

  ```
  $> cd [mvn-repo]
  $> git push https://github.com/your-name/mvn-repo
  ```
1. Edit the `pom.xml` file for the parser project:
  1. Change the `username` property to your GitHub username. e.g.
  ```xml
  <properties>
      <username>your-github-username</username>
  </properties>
  ```
  1. Uncomment the `<repository>` at the end of the file.  That is delete
  the line that starts with &lt;!-- and the line that starts with --&gt;
  
**Note** If you make changes to any of your previous assignments don't forget to push the
updated version to your `mvn-repo` on GitHub or Travis will continue to
use the old version.

Once the setup is complete you should be able to connect to [https://education.travis-ci.com](https://education.travis-ci.com),
sign in with your GitHub account and see the private repository for your
assignment listed.  If not, please post a message to the Google Group.

The final step is to tell Travis to build your project when code is pushed to GitHub:
 
1. Click the '+' next to `My Repositories`
1. Select the `Vassar CMPU 331` organization.
1. Click the slider so you have a green checkmark next to your repository.
1. Push some code to GitHub.

**Note** It has still yet to be determined if students have permission to enable builds on
Travis-CI themselves.  However, this isn't strictly necessary as we will enable
Travis on all repositories once all students have cloned their private 
repositories.
