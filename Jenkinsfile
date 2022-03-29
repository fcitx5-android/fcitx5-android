import groovy.transform.Field

@Field
def abiList = ['armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64']

@Field
def commitSha = ""

def setBuildStatus(String message, String state, String ctx, String commitSha) {
    withCredentials([string(credentialsId: 'github-commit-status-token', variable: 'token')]) {
        def body = """{
             "state": "$state",
             "description": "$message",
             "context": "$ctx",
             "target_url": "$BUILD_URL"
          }
        """.toString()
        httpRequest consoleLogResponseBody: true,
                contentType: 'APPLICATION_JSON',
                httpMode: 'POST',
                requestBody: body,
                url: "https://api.github.com/repos/fcitx5-android/fcitx5-android/statuses/$commitSha".toString(),
                validResponseCodes: '201',
                customHeaders: [[name: 'Authorization', value: "token " + token]]
    }
}

def sendMessageToTelegramGroup(String message) {
    withCredentials([string(credentialsId: 'fcitx5-android-telegram-group', variable: 'chatId'),
                     string(credentialsId: 'fcitx5-android-telegram-bot', variable: 'token')]) {
        def body = """{
             "chat_id": $chatId,
             "text": "${message.replace("-", "\\\\-")}",
             "parse_mode": "MarkdownV2",
             "disable_web_page_preview": true
          }
        """.toString()
        httpRequest consoleLogResponseBody: true,
                contentType: 'APPLICATION_JSON',
                httpMode: 'POST',
                requestBody: body,
                url: "https://api.telegram.org/bot$token/sendMessage".toString(),
                validResponseCodes: '200'
    }
}

def withBuildStatus(String name, Closure closure) {
    def ctx = "Jenkins Build / $name"
    stage(name) {
        try {
            setBuildStatus("...", "pending", ctx, commitSha)
            def start = System.currentTimeSeconds()
            closure()
            def end = System.currentTimeSeconds()
            setBuildStatus("Successful in ${end - start} seconds", "success", ctx, commitSha)
        } catch (Exception e) {
            setBuildStatus("Failed", "failure", ctx, commitSha)
            throw e
        }
    }
}

def forEachABI(String name, Closure closure) {
    abiList.each { String abi ->
        withBuildStatus("$name ($abi)") {
            closure(abi)
        }
    }
}


node("android") {
    catchError {
        timestamps {
            try {
                stage("Fetching sources") {
                    checkout([$class                           : 'GitSCM',
                              branches                         : scm.branches,
                              doGenerateSubmoduleConfigurations: false,
                              extensions                       : [[$class             : 'SubmoduleOption',
                                                                   disableSubmodules  : false,
                                                                   parentCredentials  : true,
                                                                   recursiveSubmodules: true,
                                                                   reference          : '',
                                                                   trackingSubmodules : false]],
                              submoduleCfg                     : [],
                              userRemoteConfigs                : scm.userRemoteConfigs])
                    sh "git config --get remote.origin.url > .git/remote-url"
                    repoUrl = readFile(".git/remote-url").trim()
                    sh "git rev-parse HEAD > .git/current-commit"
                    commitSha = readFile(".git/current-commit").trim()
                    setBuildStatus("...", "pending", "Jenkins Build", commitSha)
                    sh 'mkdir out'
                }

                withBuildStatus("Compile release kotlin") {
                    sh './gradlew compileReleaseKotlin'
                }

                forEachABI("Assemble release") { String abi ->
                    withEnv(["ABI=$abi"]) {
                        sh "./gradlew assembleRelease"
                        sh 'mv app/build/outputs/apk/release/*.apk out/'
                    }
                }

                withBuildStatus("Sign and archive apks") {
                    signAndroidApks(
                            keyStoreId: 'fcitx5-android-sign-key',
                            apksToSign: 'out/*-unsigned.apk',
                            archiveSignedApks: true,
                    )
                }

                stage("Post build (success)") {
                    setBuildStatus("Successful", "success", "Jenkins Build", commitSha)
                    sendMessageToTelegramGroup("[${JOB_NAME}-${BUILD_NUMBER}](${BUILD_URL}) succeeded")
                }

            } catch (Exception e) {
                stage("Post build (failure)") {
                    setBuildStatus("Failed", "failure", "Jenkins Build", commitSha)
                    sendMessageToTelegramGroup("[${JOB_NAME}-${BUILD_NUMBER}](${BUILD_URL}) failed")
                    throw e
                }
            }

        }
    }
}
