将项目发布到Jcenter Maven仓库中


2.1 发布Jcenter步骤

2.1.1 注册bintray帐号

 为了让自己的项目也能够被全世界的开发者使用，我们可以通过将lib项目发布到jcenter库中，在配置脚本之前我们需要先去官网注册一个帐号，传送门：bintray https://bintray.com/ 也可以使用第三方登录的方式来登录，包括github、google、facebook帐号等。注册成功后我们先要获取到一个api key。
这里写图片描述

2.1.2 上传文件

 在Jcenter库中要求上传到库中的项目必须包含4个文件：
   - javadoc.jar
   - sources.jar
   - aar或者jar
   - pom
  如果少了审核可能不会通过，当然这几个文件都可一通过配置gradle脚本来自动生成。

2.2 配置Gradle脚本

 为了创建上面所说的几个文件，我们需要构建脚本来自动生成对应的文件。具体可以参考：github-SwipeView-build.gradle

2.2.1 配置项目依赖

将项目文件根目录（Top-level）下面的buide.gradle文件增加依赖：

 dependencies {
        classpath 'com.android.tools.build:gradle:1.0.0'
        classpath 'com.github.dcendents:android-maven-plugin:1.2'
        classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.0'

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }1
2
3
4
5
6
7
8
1
2
3
4
5
6
7
8


注意： classpath ‘com.android.tools.build:gradle:1.0.0’ 在默认生成的文件下可能版本不一致，采用默认的有时候会导致构建失败，最好也修改成1.0.0版本的。

2.2.2 增加gradle插件和版本号

 在需要上传的library项目的build.gradle下增加插件引用和版本号：

apply plugin: 'com.android.library'
apply plugin: 'com.github.dcendents.android-maven'
apply plugin: 'com.jfrog.bintray'
version = "1.0"1
2
3
4
1
2
3
4


注意：版本号作为项目坐标的一部分，以后在升级项目的时候都需要升级版本号，否则会覆盖掉已经上传的版本.
  关于插件bintray的更详细的使用方式可以查看：github-bintray

2.2.3 pom节点生成

 生成POM文件build脚本

def siteUrl = 'https://github.com/daliyan/SwipeView'      // 项目的主页
def gitUrl = 'https://github.com/daliyan/SwipeView.git'   // Git仓库的url
group = "akiyama.swipe"
// 根节点添加
install {
    repositories.mavenInstaller {
        // This generates POM.xml with proper parameters
        pom {
            project {
                packaging 'aar'
                name 'swipeView For Android'
                url siteUrl
                licenses {
                    license {
                        name 'The Apache Software License, Version 2.0'
                        url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                    }
                }
                developers {
                    developer {
                        id 'akiyama'
                        name 'daliyan'
                        email 'dali_yan@yeah.net'
                    }
                }
                scm {
                    connection gitUrl
                    developerConnection gitUrl
                    url siteUrl
                }
            }
        }
    }
}1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
34
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
34


注意：group = “akiyama.swipe”作为项目坐标的前缀，packaging ‘aar’ 为arr包，其它的自己随意填写。

2.2.4 javadoc和sources文件的生成

 添加生成任务：

task sourcesJar(type: Jar) {
    from android.sourceSets.main.java.srcDirs
    classifier = 'sources'
}

task javadoc(type: Javadoc) {
    source = android.sourceSets.main.java.srcDirs
    classpath += project.files(android.getBootClasspath().join(File.pathSeparator))
}

task javadocJar(type: Jar, dependsOn: javadoc) {
    classifier = 'javadoc'
    from javadoc.destinationDir
}

artifacts {
    archives javadocJar
    archives sourcesJar
}1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19


注意：在构建生成的时候有可能会报GBK编码等错误，可能需要添加UTF-8声明，如下：

//添加UTF-8编码否则注释可能JAVADOC文档可能生成不了
javadoc {
    options{
        encoding "UTF-8"
        charSet 'UTF-8'
        author true
        version true
        links "http://docs.oracle.com/javase/7/docs/api"
        title "swipeJavaDoc"
    }
}1
2
3
4
5
6
7
8
9
10
11
1
2
3
4
5
6
7
8
9
10
11

2.2.5 构建上传jecnter库中脚本

 使用前面的我们注册帐号和apikey上传对应的文件到jcenter库中：

Properties properties = new Properties()
properties.load(project.rootProject.file('local.properties').newDataInputStream())
bintray {
    user = properties.getProperty("bintray.user")
    key = properties.getProperty("bintray.apikey")
    configurations = ['archives']
    pkg {
        repo = "maven"
        name = "swipeView"                // project name in jcenter
        websiteUrl = siteUrl
        vcsUrl = gitUrl
        licenses = ["Apache-2.0"]
        publish = true
    }
}1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15

 因为用户名和apikey是属于个人的隐私信息，故在local.properties（该文件不会上传到git库中）本地文件中配置用户名和apikey

sdk.dir=/home/android-sdk
bintray.user=your username
bintray.apikey=your apikey1
2
3
1
2
3

2.3 上传和审核

在配置好了上述build.gradle文件后我们打开gradle控制面板就能看到如图所示的构建任务：
这里写图片描述

这里写图片描述

我们只需要双击bintrayUpload就能自动上传到jcenter库中了。
 到官网找到我们刚刚上传的文件，提交审核就行了（别跟我说你找不到），一般2-3个小时就能审核成功。
 成功后可以通过http://jcenter.bintray.com/ 查询到你的库文件，例如我的项目文件路径为：http://jcenter.bintray.com/akiyama/swipe/library/2.1

2.4 同步项目到mvnrepository

在jcenter中提供了将项目同步到mvnrepository库中,这样就不需要操作上传到mvnrepository库的繁琐步骤。在bintray构建脚本最后加上：

 //Optional configuration for Maven Central sync of the version
            mavenCentralSync {
                sync = true //Optional (true by default). Determines whether to sync the version to Maven Central.
                user = 'userToken' //OSS user token
                password = 'paasword' //OSS user password
                close = '1' //Optional property. By default the staging repository is closed and artifacts are released to Maven Central. You can optionally turn this behaviour off (by puting 0 as value) and release the version manually.
            } 1
2
3
4
5
6
7
1
2
3
4
5
6
7


注意：user和password即为mvnrepository中注册的用户名和密码。如果同步成功你也可以通过http://mvnrepository.com/ 查询到你上传的lib项目

2.5 常见问题

在构建脚本过程中可能会出现一些问题：
 - GBK编码问题，前文已经提供了解决方案；
 - 依赖库问题，可能会报告一些警告，只要保证最后构建成功，直接忽略即可；
 - gradle依赖问题：可以参照githug-bintray 解决方案：
 Gradle >= 2.1

plugins {
    id "com.jfrog.bintray" version "1.3.1"
}1
2
3
1
2
3

Gradle < 2.1

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.3.1'
    }
}
apply plugin: 'com.jfrog.bintray'1
2
3
4
5
6
7
8
9
1
2
3
4
5
6
7
8
9

3 小结

本文学习了gradle的一些基本知识和基本的构建，学习了如何将lib库上传到中央仓库中，以及在这个过程中可能遇到的问题。
