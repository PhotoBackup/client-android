# Changelog

# 0.9.0
* [4a9e39c](https://github.com/PhotoBackup/client-android/commit/4a9e39c) Switch from loopj to OkHttp (to handle HTTPS SNI #23 and redirection #15) ;
* [67eeb00](https://github.com/PhotoBackup/client-android/commit/67eeb00) Async update of Upload Log ui after manual upload ;
* [71e7dee](https://github.com/PhotoBackup/client-android/commit/71e7dee) Saving the error message per photo and display it under the file name ;
* [c5cfe04](https://github.com/PhotoBackup/client-android/commit/c5cfe04) New German translation thanks to @zealot128 ;
* [0892104](https://github.com/PhotoBackup/client-android/commit/0892104) New Czech translation thanks to @svetlemodry ;
* [ca629b3](https://github.com/PhotoBackup/client-android/commit/ca629b3) handling 409 status code properly #45 ;
* [1108063](https://github.com/PhotoBackup/client-android/commit/1108063) remove 'Stop the service' action in the notification #41 ;
* some Sonar-induced fixes ([c8ccce8](https://github.com/PhotoBackup/client-android/commit/c8ccce8), [af13b2c](https://github.com/PhotoBackup/client-android/commit/af13b2c), [f0f8508](https://github.com/PhotoBackup/client-android/commit/f0f8508) and [8ce3611](https://github.com/PhotoBackup/client-android/commit/8ce3611)) ;
* finally, an operational Travis build: https://travis-ci.org/PhotoBackup/client-android ;
* for developers, a gitter channel to discuss: https://gitter.im/PhotoBackup


# 0.8.0
* [3c4f128](https://github.com/PhotoBackup/client-android/commit/3c4f128) Czech translation
* [47a0c62](https://github.com/PhotoBackup/client-android/commit/47a0c62) Add multiple servers capability basic architecture

## 0.7.2
* [dae700c](https://github.com/PhotoBackup/client-android/commit/dae700c) Fix 0.6 => 0.7 migration by setting it before addPreferencesFromResources

## 0.7.1
* [29ac3dc](https://github.com/PhotoBackup/client-android/commit/29ac3dc) Remove Handler messages for recycled views 
* [2080fc0](https://github.com/PhotoBackup/client-android/commit/2080fc0) Fix 'Only wifi' preference type bug in v0.7.0 by adding a migration

## 0.7.0
* [c5f5cab](https://github.com/PhotoBackup/client-android/commit/c5f5cab) Add an option to upload only recently taken pictures.
* [4321481](https://github.com/PhotoBackup/client-android/commit/4321481) Add filter to journal activity
* [7cf98d5](https://github.com/PhotoBackup/client-android/commit/7cf98d5) Set delay to new photos up to 10 minutes
* [a07628f](https://github.com/PhotoBackup/client-android/commit/a07628f) Fix erroneous number of pictures in journal entry

## 0.6.5
* [b90deb4](https://github.com/PhotoBackup/client-android/commit/b90deb4)  Support Android M runtime permissions
* [b7bf565](https://github.com/PhotoBackup/client-android/commit/b7bf565)  Update AAHC to version 1.4.9
* [84e540a](https://github.com/PhotoBackup/client-android/commit/84e540a) Move permission callback from fragment to activity

## 0.6.4
* [47e5ce3](https://github.com/PhotoBackup/client-android/commit/47e5ce3)  Add notification action to stop the service from it.
* [4da4364](https://github.com/PhotoBackup/client-android/commit/4da4364) Update for Android M

## 0.6.3
* [2dd370a](https://github.com/PhotoBackup/client-android/commit/2dd370a) Fix bug: boot receiver not reading user-defined prefernce

## 0.6.2
* [eada898](https://github.com/PhotoBackup/client-android/commit/eada898)  Fix bug #3 by removing slashes at the end of URLs
* [8b37bbb](https://github.com/PhotoBackup/client-android/commit/8b37bbb)  Fix #2: set network choosing as a modal single choice dialog
* [a689da7](https://github.com/PhotoBackup/client-android/commit/a689da7) Update error icon

## 0.6.1
* [5e368d0](https://github.com/PhotoBackup/client-android/commit/5e368d0) Fix password hash when changing password
* [b5ef6d4](https://github.com/PhotoBackup/client-android/commit/b5ef6d4) Add screenshot image
* [c0aeccb](https://github.com/PhotoBackup/client-android/commit/c0aeccb) Remove unused text
* [10f0faf](https://github.com/PhotoBackup/client-android/commit/10f0faf) Add README.md

## 0.6
* [163f604](https://github.com/PhotoBackup/client-android/commit/163f604) Add an 'About' activity to explain quickly the purpose of the app and

## 0.5
* [7747624](https://github.com/PhotoBackup/client-android/commit/7747624) Add file size in the request parameter

## 0.4
* [6e1ca37](https://github.com/PhotoBackup/client-android/commit/6e1ca37) Initial commit

