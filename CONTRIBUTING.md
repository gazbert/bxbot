# Contributing to BX-bot
BX-bot is released under the [MIT](http://opensource.org/licenses/MIT) license. 
If you would like to contribute something, or simply want to hack the code, this document should 
help you get started.
 
## Reporting bugs and suggesting enhancements
If you find a bug, please submit an [issue](https://github.com/gazbert/bxbot/issues). Try to 
provide enough information for someone else to reproduce it. Equally, submit an issue if you want 
to create a new feature or enhance an existing one - refactoring, improving Javadoc, and boosting 
test coverage is always welcome!

One of the project's maintainers should respond to your issue within 48 hours... if not, bump the 
issue and request that it be reviewed.

## Contributor workflow

Review the [issues list](https://github.com/gazbert/bxbot/issues) and find something that 
interests you. It is wise to start with something relatively straightforward and achievable. 
Usually there will be a comment in the issue that indicates whether someone has already 
self-assigned it. If no one has already taken the issue, then add a comment assigning the issue to 
yourself, e.g. ```I'm working on this issue because I love writing Javadoc...``` 

Please be considerate and rescind the offer in a comment if you cannot finish in a reasonable time,
or add a new comment stating that you're still actively working on the issue and need a little 
more time.

We are using the [GitHub Flow](https://guides.github.com/introduction/flow/) process to manage 
code contributions. If you are unfamiliar, please review that link before proceeding. 
To work on something, whether a new feature or a bug fix:

  1. [Fork](https://help.github.com/articles/fork-a-repo/) the repo.
  2. Clone it locally:
  
  ```
  git clone https://github.com/<your-id>/bxbot.git
  ```
  3. Add the upstream repository as a remote: 
  ```
  git remote add upstream https://github.com/gazbert/bxbot.git
  ```  
  Make sure you keep your forked repo [up-to-date](https://help.github.com/articles/syncing-a-fork/) 
  with the upstream repository.
  
  4. Create a meaningful-named branch off of your cloned fork - full details 
  [here](https://git-scm.com/docs/git-checkout).
  
  ```
  cd bxbot
  git checkout -b my-new-feature-or-bugfix-branch
  ```
  5. Write some code! Commit to that branch locally, and regularly push your work to the same 
  branch on the server. Commit messages must have a short description no longer than 50 characters 
  followed by a blank line and a longer, more descriptive message that includes reference to 
  issue(s) being addressed so that they will be automatically closed on a merge e.g. 
  ```Closes #1234``` or ```Fixes #1234``` - see 
  [here](https://help.github.com/articles/closing-issues-via-commit-messages/) for more details.
  When writing a commit message please follow 
  [these conventions](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html).

  6. Any code changes that affect documentation (e.g. README.MD) should be accompanied by 
  corresponding changes (or additions) to the documentation and tests. This will ensure that if 
  the merged PR is reversed, all traces of the change will be reversed as well.

  7. When you're done, make sure your [Travis](https://travis-ci.org/) 
  Continuous Integration (CI) build is green and create a 
  [Pull Request (PR)](https://help.github.com/articles/using-pull-requests/) to submit your changes.

  
After your PR has been reviewed and signed off, a maintainer will merge it into 
the master branch.

## Code conventions and housekeeping

The following conventions should be followed:

* Make sure all new `.java` files to have a simple Javadoc class comment with at least an
  `@author` tag identifying you, and preferably at least a paragraph on what the class is for.
  All public API methods should have detailed Javadoc describing the contract and what they do.
* Add the MIT license header comment to all new `.java` files - copy from existing files in the 
  project.
* Did we mention tests? All code changes should be accompanied by new or modified tests. 
  Someone has to do it...
* Add yourself as an `@author` to the `.java` files that you modify substantially 
  (more than cosmetic changes).
* In general commits should be atomic and diffs should be easy to read. For this reason do not mix 
  any formatting fixes or code moves with actual code changes.
* We use the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) for 
  formatting our code. You can download the style template for your IDE from 
  [here](https://github.com/google/styleguide). If you use IntelliJ, there's also a handy 
  [Google plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format) for formatting your 
  code.
* We like static analysis: the build uses [JaCoCo](https://www.jacoco.org/jacoco/) to enforce code 
  coverage and [SpotBugs](https://spotbugs.github.io/) to weed out bugs. 
  [Sonarcloud](https://sonarcloud.io/dashboard?id=gazbert_bxbot) is used for analysing and 
  maintaining the quality of our code. Please maintain this, or better still, help improve it! 