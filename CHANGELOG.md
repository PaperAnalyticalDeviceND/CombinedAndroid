# Changelog

## [1.5] - 2021-12-20

### Bug Fixes

- Added crash reporting for failing to load models ([29ca03b](29ca03bd914dfae22b5b4087f10ff4900da4ec0b))

### Refactor

- Pulled model loading into its own class and utilized viewmodel to handle predictions ([fc4f1e7](fc4f1e717ba72d6a86870612fe1cf08e2d52a374))
- Updated pls creation function and handle it in the viewmodel for predictions ([412a147](412a147e547333206c62e07a81d8063a5d01b1a0))

## [1.4] - 2021-12-14

### Bug Fixes

- *(firebase)* Change to fix Service not registered error ([b2f3b89](b2f3b89a45d075f55bc8101854b5c9c3dd6fb0a3))
- *(gradle)* Remove need for setting specific path for opencv ([3b40087](3b400872102f01bb457b6d4d433de4e0586a446a))
- *(gradle)* Updated android gradle tools version ([5dfd369](5dfd369e9d175cadbe36b15351d9efa89f959ed8))
- *(queue)* Updated androidx work libraries to 2.7.0 to fix a bug with newer android versions ([44c6016](44c6016c2c07027666ded0a9b49728657d736ff7))
- #35, #36, #37, #38 ([67d4a83](67d4a831a3408b81c5c93292d41a7524ae684477))
- Delete QueueActivity since it's not used. ([2a4a398](2a4a398e77516b53673256b39689c64c33d8d1c8))
- Deleting old camera classes ([8d326be](8d326be834400ddff959ffd3e949acbcf5f6c3a8))
- Missing semicolon breaking build ([59f3912](59f39122dc25439e587c1852070a9c4d6437f031))

### Features

- *(firebase)* Added crash logging, event tracking and performance tracking ([99c281d](99c281df80111069d031703c433d4df77c2f919c))
- *(gradle)* Cleanup gradle files to use newer versions and solve the listener dependency issue ([681c966](681c96684a5c044b712ccca91de1ef1fe7778d56))
- *(zip)* Incorporate code from commonsguy/cwac-security to handle zip traversal security issue ([1e2c671](1e2c6710dea93f1647f0e26ccf951e86bdff3534))

### Miscellaneous Tasks

- *(repository)* Remove opencv build files from repository ([6a9a89c](6a9a89c399be3b3a59190555fbc1b3e72b1a0f3e))

### Refactor

- Cleaned unused imports ([bd14b37](bd14b37d7220570e61c5a8593f4f5508ba3cfb12))
- Remove commented out code ([822bed8](822bed8358452acfec2ee5d78d23ede1f2e8cfb6))
- Reformat code using android studio ([5b58d5f](5b58d5f438d02203bc32073edec397391acab1c7))
- Fix a large number of linting messages from android studio ([cca0c45](cca0c45725e0571f3b400746ab59739c12e4814e))
- Update code styles setup (suggested by android-studio) ([a633389](a633389abf3d2e9ec46b2d21c3c78b6e7da6205f))

