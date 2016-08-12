# Contributing to BX-Bot
BX-Bot is released under the [MIT](http://opensource.org/licenses/MIT) license. 
If you would like to contribute something, or simply want to hack the code, this document should help get you started.
 
## Reporting Bugs and Suggesting Enhancements
If you find a bug, please submit an [issue](https://github.com/gazbert/BX-bot/issues). Try to provide enough information
for someone else to reproduce the issue. Equally, submit an issue if you want to create a new feature or enhance an
existing one - refactoring, improving Javadoc, and boosting test coverage is always welcome!

One of the project's maintainers should respond to your issue within 48 hours.
If not, please bump the issue and request that it be reviewed.

## Contributor Workflow

Review the [issues list](https://github.com/gazbert/BX-bot/issues) and find something that interests you. 
It is wise to start with something relatively straight forward and achievable. Usually there will be a comment in the 
issue that indicates whether someone has already self-assigned the issue. If no one has already taken it, then add a 
comment assigning the issue to yourself, e.g. ```I'll work on this issue.```. 

Please be considerate and rescind the offer in a comment if you cannot finish in a reasonable time. 
Or add a new comment saying that you are still actively working the issue if you need a little more time.

We are using the [GitHub Flow](https://guides.github.com/introduction/flow/) process to manage code contributions. 
If you are unfamiliar, please review that link before proceeding. 
To work on something, whether a new feature or a bug fix:

  1. [Fork](https://help.github.com/articles/fork-a-repo/) the repo.
  2. Clone it locally:
  
  ```
  git clone https://github.com/<your-id>/BX-bot.git
  ```
  3. Add the upstream repository as a remote:
  
  ```
  git remote add upstream https://github.com/gazbert/BX-bot.git
  ```
  4. Create a descriptively-named branch off of your cloned fork - full details [here](https://git-scm.com/docs/git-checkout).
  
  ```
  cd BX-bot;
  git checkout -b <issue-xxx> <feature-name>
  ```
  5. Commit your code

  Do some coding! Commit to that branch locally, and regularly push your work to the same branch on the server.

  6. Commit messages

  Commit messages must have a short description no longer than 50 characters followed by a blank line and a longer,
  more descriptive message that includes reference to issue(s) being addressed so that they will be automatically closed
  on a merge e.g. ```Closes #1234``` or ```Fixes #1234```.
  
  When writing a commit message please follow [these conventions](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html).

  7. Pull Request (PR)

   Make sure your [Travis](https://travis-ci.org/) Continuous Integration (CI) build is green and create a 
   [Pull Request](https://help.github.com/articles/using-pull-requests/) when you ready to submit your changes.

   _NOTE: If your PR does not merge cleanly, use ```git rebase master``` in your feature branch to update your pull 
   request rather than using ```git merge master```._

  10. Any code changes that affect documentation (e.g. README.MD) should be accompanied by corresponding changes
   (or additions) to the documentation and tests. This will ensure that if the merged PR is reversed, all traces of the
    change will be reversed as well.

After your Pull Request (PR) has been reviewed and signed off, a maintainer will merge it into the master branch.

### Code Conventions and Housekeeping

These convention should (ideally!) be followed:

* Make sure all new `.java` files to have a simple Javadoc class comment with at least an
  `@author` tag identifying you, and preferably at least a paragraph on what the class is for.
* Add the MIT license header comment to all new `.java` files - copy from existing files in the project.
* Did we mention tests? All code changes should be accompanied by new or modified tests.
* Add yourself as an `@author` to the `.java` files that you modify substantially (more than cosmetic changes).
* Add some Javadocs.
* In general commits should be atomic and diffs should be easy to read. For this reason do not mix any formatting fixes 
  or code moves with actual code changes.

### Squashing Commits

If your pull request is accepted for merging, you may be asked by a maintainer to squash and or 
[rebase](https://git-scm.com/docs/git-rebase) your commits before it will be merged. 
The basic squashing workflow is shown below:

    git checkout your_branch_name
    git rebase -i HEAD~n
    # n is normally the number of commits in the pull
    # set commits from 'pick' to 'squash', save and quit
    # on the next screen, edit/refine commit messages
    # save and quit
    git push -f # (force push to GitHub)