package tests.commands

class ExportCommandSuite extends tests.BaseSuite with tests.ConfigSyntax {

  checkDeps(
    "transitive version ranges do not show up",
    s"""|  - dependency: com.nimbusds:nimbus-jose-jwt:4.41.1
        |""".stripMargin,
    queryArgs = allGenrules,
    expectedQuery = """|@maven//:genrules/com.github.stephenc.jcip_jcip-annotations_1.0-1
                       |@maven//:genrules/com.nimbusds_nimbus-jose-jwt_4.41.1
                       |@maven//:genrules/net.minidev_accessors-smart_1.2
                       |@maven//:genrules/net.minidev_json-smart_2.3
                       |@maven//:genrules/org.ow2.asm_asm_5.0.4
                       |""".stripMargin
  )

  checkDeps(
    "evicted artifacts do not create genrules",
    s"""|  - dependency: org.slf4j:slf4j-log4j12:1.6.1
        |    force: true
        |  - dependency: org.slf4j:slf4j-log4j12:1.6.4
        |    force: false
        |""".stripMargin,
    arguments = exportCommand,
    queryArgs = allGenrules,
    expectedQuery = """|@maven//:genrules/log4j_log4j_1.2.16
                       |@maven//:genrules/org.slf4j_slf4j-api_1.6.1
                       |@maven//:genrules/org.slf4j_slf4j-log4j12_1.6.1
                       |""".stripMargin
  )

  checkDeps(
    "evicted artifacts do not create genrules without forces",
    s"""|  - dependency: org.slf4j:slf4j-log4j12:1.6.1
        |    targets: [slf4j-1.6.1]
        |  - dependency: org.slf4j:slf4j-log4j12:1.6.4
        |    targets: [slf4j-1.6.4]
        |""".stripMargin,
    arguments = exportCommand :+ "--no-fail-on-evicted-declared",
    queryArgs = allGenrules,
    expectedQuery = """|@maven//:genrules/log4j_log4j_1.2.16
                       |@maven//:genrules/org.slf4j_slf4j-api_1.6.4
                       |@maven//:genrules/org.slf4j_slf4j-log4j12_1.6.4
                       |""".stripMargin,
    expectedOutput =
      """|/workingDirectory/3rdparty.yaml:3:16 warning: Declared third party dependency 'org.slf4j:slf4j-log4j12:1.6.1' is evicted in favor of 'org.slf4j:slf4j-log4j12:1.6.4'.
         |Update the third party declaration to use version '1.6.4' instead of '1.6.1' to reflect the effective dependency graph.
         |Info:
         |  'org.slf4j:slf4j-log4j12:1.6.1' is declared in slf4j-1.6.1.
         |  'org.slf4j:slf4j-log4j12:1.6.4' is a transitive dependency of slf4j-1.6.4.
         |  - dependency: org.slf4j:slf4j-log4j12:1.6.1
         |                ^
         |warning: 1 declared dependency was evicted.
         |""".stripMargin + defaultExpectedOutput
  )

  checkDeps(
    "scalatest",
    """|  - dependency: org.scalatest:scalatest_2.12:3.1.2
       |    targets: [scalatest]
       |""".stripMargin,
    // queryArgs = allScalaImports,
    queryArgs = allJars("@maven//:scalatest"),
    expectedQuery = """|@maven//:org.scala-lang.modules/scala-xml_2.12/1.2.0.jar
                       |@maven//:org.scala-lang/scala-library/2.12.11.jar
                       |@maven//:org.scala-lang/scala-reflect/2.12.11.jar
                       |@maven//:org.scalactic/scalactic_2.12/3.1.2.jar
                       |@maven//:org.scalatest/scalatest_2.12/3.1.2.jar
                       |""".stripMargin
  )

  checkDeps(
    "netty",
    """|  - dependency: io.netty:netty:3.10.1.Final
       |    targets: [netty-310]
       |  - dependency: io.netty:netty:3.7.0.Final
       |    targets: [netty-37]
       |""".stripMargin,
    arguments = exportCommand :+ "--no-fail-on-evicted-declared",
    queryArgs = allGenrules,
    expectedQuery = """|@maven//:genrules/io.netty_netty_3.10.1.Final
                       |""".stripMargin,
    expectedOutput =
      """|/workingDirectory/3rdparty.yaml:5:16 warning: Declared third party dependency 'io.netty:netty:3.7.0.Final' is evicted in favor of 'io.netty:netty:3.10.1.Final'.
         |Update the third party declaration to use version '3.10.1.Final' instead of '3.7.0.Final' to reflect the effective dependency graph.
         |Info:
         |  'io.netty:netty:3.7.0.Final' is declared in netty-37.
         |  'io.netty:netty:3.10.1.Final' is a transitive dependency of netty-310.
         |  - dependency: io.netty:netty:3.7.0.Final
         |                ^
         |warning: 1 declared dependency was evicted.
         |""".stripMargin + defaultExpectedOutput
  )

  checkDeps(
    "basic",
    """|  - dependency: com.google.guava:guava:29.0-jre
       |  - dependency: org.apiguardian:apiguardian-api:1.1.1
       |    targets: [apiguardian]
       |  - dependency: org.eclipse.lsp4j:org.eclipse.lsp4j:0.9.0
       |    dependencies: [apiguardian]
       |""".stripMargin,
    expectedOutput = """|/workingDirectory/3rdparty.yaml:3:16 error: transitive dependency 'com.google.guava:guava' has conflicting versions.
         |       found versions: 27.1-jre
         |    declared versions: 29.0-jre
         |      popular version: 29.0-jre
         |              ok deps: com.google.guava:guava:29.0-jre
         |           ok targets:
         |       unpopular deps: org.eclipse.lsp4j:org.eclipse.lsp4j:0.9.0
         |    unpopular targets:
         |  To fix this problem, add 'dependency = com.google.guava:guava:27.1-jre' to the root dependencies OR add 'targets' to the transitive dependency.
         |  - dependency: com.google.guava:guava:29.0-jre
         |                ^""".stripMargin,
    expectedExit = 1
  )

  checkDeps(
    "libthrift",
    """|  - dependency: org.apache.thrift:libthrift:0.10.0
       |""".stripMargin
  )

  checkDeps(
    "version_scheme",
    s"""|  - dependency: com.lihaoyi:fansi_2.12:0.2.8
        |    versionScheme: pvp
        |  - dependency: com.lihaoyi:sourcecode_2.12:0.2.0
        |    versionScheme: pvp
        |  - dependency: com.lihaoyi:pprint_2.12:0.5.9
        |${scalaLibrary("MyApp.scala", "object MyApp { val x = 42 }")}
        |""".stripMargin
  )

  checkDeps(
    "classifier",
    s"""|  - dependency: jline:jline:2.14.6
        |  - dependency: jline:jline:2.14.6
        |    classifier: test
        |""".stripMargin
  )

  checkDeps(
    "classifier with eviction",
    s"""|  - dependency: org.apache.kafka:kafka-clients:2.4.1
        |    versionScheme: pvp
        |    targets: [client-2.4.1]
        |  - dependency: org.apache.kafka:kafka-clients:2.4.0
        |    versionScheme: pvp
        |    classifier: test
        |    targets: [client-2.4.0]
        |""".stripMargin,
    arguments = exportCommand :+ "--no-fail-on-evicted-declared",
    queryArgs = allGenrules,
    expectedQuery = """|@maven//:genrules/com.github.luben_zstd-jni_1.4.3-1
                       |@maven//:genrules/org.apache.kafka_kafka-clients_2.4.1
                       |@maven//:genrules/org.apache.kafka_kafka-clients_2.4.1_test
                       |@maven//:genrules/org.lz4_lz4-java_1.6.0
                       |@maven//:genrules/org.slf4j_slf4j-api_1.7.28
                       |@maven//:genrules/org.xerial.snappy_snappy-java_1.1.7.3
                       |""".stripMargin,
    expectedOutput =
      """|/workingDirectory/3rdparty.yaml:6:16 warning: Declared third party dependency 'org.apache.kafka:kafka-clients:2.4.0' is evicted in favor of 'org.apache.kafka:kafka-clients:2.4.1'.
         |Update the third party declaration to use version '2.4.1' instead of '2.4.0' to reflect the effective dependency graph.
         |Info:
         |  'org.apache.kafka:kafka-clients:2.4.0' is declared in client-2.4.0.
         |  'org.apache.kafka:kafka-clients:2.4.1' is a transitive dependency of client-2.4.1.
         |  - dependency: org.apache.kafka:kafka-clients:2.4.0
         |                ^
         |warning: 1 declared dependency was evicted.
         |""".stripMargin + defaultExpectedOutput,
  )

  checkMultipleDeps(
    "classifier with eviction 2",
    s"""|  - dependency: org.apache.kafka:kafka-clients:2.5.0
        |    versionScheme: pvp
        |    targets: [client-2.5.0]
        |  - dependency: org.apache.kafka:kafka-clients:2.4.0
        |    versionScheme: pvp
        |    classifier: test
        |    targets: [client-2.4.0]
        |""".stripMargin,
    queries = List(
      allScalaImportDeps("@maven//:client-2.5.0") ->
        """|@maven//:_com.github.luben_zstd-jni_1.4.4-7
           |@maven//:_org.lz4_lz4-java_1.7.1
           |@maven//:_org.slf4j_slf4j-api_1.7.30
           |@maven//:_org.xerial.snappy_snappy-java_1.1.7.3
           |@maven//:client-2.5.0
           |@maven//:org.apache.kafka_kafka-clients_2.5.0_1493927235
           |""".stripMargin,
      allScalaImportDeps("@maven//:client-2.4.0") ->
        """|@maven//:_com.github.luben_zstd-jni_1.4.4-7
           |@maven//:_org.lz4_lz4-java_1.7.1
           |@maven//:_org.slf4j_slf4j-api_1.7.30
           |@maven//:_org.xerial.snappy_snappy-java_1.1.7.3
           |@maven//:client-2.4.0
           |@maven//:org.apache.kafka_kafka-clients_2.4.0_test_1232654558
           |""".stripMargin
    )
  )

  checkDeps(
    "kafka-streams",
    s"""|  - dependency: org.apache.kafka:kafka-streams:2.4.1
        |    versionScheme: pvp
        |    targets: [kafka-streams]
        |""".stripMargin,
    queryArgs = allJars("@maven//:kafka-streams"),
    expectedQuery = """|@maven//:com.fasterxml.jackson.core/jackson-annotations/2.10.0.jar
                       |@maven//:com.fasterxml.jackson.core/jackson-core/2.10.0.jar
                       |@maven//:com.fasterxml.jackson.core/jackson-databind/2.10.0.jar
                       |@maven//:com.fasterxml.jackson.datatype/jackson-datatype-jdk8/2.10.0.jar
                       |@maven//:com.github.luben/zstd-jni/1.4.3-1.jar
                       |@maven//:org.apache.kafka/connect-api/2.4.1.jar
                       |@maven//:org.apache.kafka/connect-json/2.4.1.jar
                       |@maven//:org.apache.kafka/kafka-clients/2.4.1.jar
                       |@maven//:org.apache.kafka/kafka-streams/2.4.1.jar
                       |@maven//:org.lz4/lz4-java/1.6.0.jar
                       |@maven//:org.rocksdb/rocksdbjni/5.18.3.jar
                       |@maven//:org.slf4j/slf4j-api/1.7.28.jar
                       |@maven//:org.xerial.snappy/snappy-java/1.1.7.3.jar""".stripMargin
  )

  checkDeps(
    "classifier with eviction dependencies",
    s"""|  - dependency: org.apache.kafka:kafka-clients:2.4.1
        |    versionScheme: pvp
        |    targets: [kafka-clients-2.4.1]
        |  - dependency: org.apache.kafka:kafka-clients:2.4.0
        |    versionScheme: pvp
        |    classifier: test
        |    targets: [kafka-clients-2.4.0]
        |  - dependency: org.apache.kafka:kafka-streams:2.4.0
        |    versionScheme: pvp
        |    targets: [kafka-streams-2.4.0]
        |""".stripMargin,
    arguments = exportCommand :+ "--no-fail-on-evicted-declared",
    queryArgs = allJars("@maven//:kafka-streams-2.4.0"),
    expectedQuery = """|@maven//:com.fasterxml.jackson.core/jackson-annotations/2.10.0.jar
                       |@maven//:com.fasterxml.jackson.core/jackson-core/2.10.0.jar
                       |@maven//:com.fasterxml.jackson.core/jackson-databind/2.10.0.jar
                       |@maven//:com.fasterxml.jackson.datatype/jackson-datatype-jdk8/2.10.0.jar
                       |@maven//:com.github.luben/zstd-jni/1.4.3-1.jar
                       |@maven//:org.apache.kafka/connect-api/2.4.0.jar
                       |@maven//:org.apache.kafka/connect-json/2.4.0.jar
                       |@maven//:org.apache.kafka/kafka-clients/2.4.1.jar
                       |@maven//:org.apache.kafka/kafka-streams/2.4.0.jar
                       |@maven//:org.lz4/lz4-java/1.6.0.jar
                       |@maven//:org.rocksdb/rocksdbjni/5.18.3.jar
                       |@maven//:org.slf4j/slf4j-api/1.7.28.jar
                       |@maven//:org.xerial.snappy/snappy-java/1.1.7.3.jar
                       |""".stripMargin,
    expectedOutput =
      """|/workingDirectory/3rdparty.yaml:6:16 warning: Declared third party dependency 'org.apache.kafka:kafka-clients:2.4.0' is evicted in favor of 'org.apache.kafka:kafka-clients:2.4.1'.
         |Update the third party declaration to use version '2.4.1' instead of '2.4.0' to reflect the effective dependency graph.
         |Info:
         |  'org.apache.kafka:kafka-clients:2.4.0' is declared in kafka-clients-2.4.0.
         |  'org.apache.kafka:kafka-clients:2.4.1' is a transitive dependency of kafka-clients-2.4.1.
         |  - dependency: org.apache.kafka:kafka-clients:2.4.0
         |                ^
         |warning: 1 declared dependency was evicted.
         |""".stripMargin + defaultExpectedOutput
  )

  checkMultipleDeps(
    "target dependency are transitively included",
    deps(
      dep("commons-logging:commons-logging:1.2")
        .target("commons-logging")
        .dependency("commons-codec"),
      dep("commons-codec:commons-codec:1.9")
        .target("commons-codec")
        .dependency("apiguardian"),
      dep("org.apiguardian:apiguardian-api:1.1.1")
        .target("apiguardian")
    ),
    queries = List(
      allJars("@maven//:commons-logging") ->
        """|@maven//:commons-codec/commons-codec/1.9.jar
           |@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin,
      allJars("@maven//:commons-codec") ->
        """|@maven//:commons-codec/commons-codec/1.9.jar
           |@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin,
      allJars("@maven//:apiguardian") ->
        """|@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin,
    )
  )

  checkMultipleDeps(
    "exclusions of target dependencies are respected",
    deps(
      dep("org.apiguardian:apiguardian-api:1.1.1")
        .target("apiguardian")
        .dependency("httpclient"),
      dep("org.apache.httpcomponents:httpclient:4.4.1")
        .target("httpclient")
        .exclude("commons-codec:commons-codec")
    ),
    queries = List(
      allJars("@maven//:httpclient") ->
        """|@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apache.httpcomponents/httpclient/4.4.1.jar
           |@maven//:org.apache.httpcomponents/httpcore/4.4.1.jar""".stripMargin,
      allJars("@maven//:apiguardian") ->
        """|@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apache.httpcomponents/httpclient/4.4.1.jar
           |@maven//:org.apache.httpcomponents/httpcore/4.4.1.jar
           |@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin,
    )
  )

  checkMultipleDeps(
    "exclusions are respected and don't leak to other resolutions",
    deps(
      dep("org.apache.thrift:libthrift:0.10.0")
        .target("libthrift")
        .exclude("commons-codec:commons-codec"),
      dep("org.apache.httpcomponents:httpclient:4.4.1")
        .target("httpclient")
    ),
    queries = List(
      allJars("@maven//:libthrift") ->
        """|@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apache.httpcomponents/httpclient/4.4.1.jar
           |@maven//:org.apache.httpcomponents/httpcore/4.4.1.jar
           |@maven//:org.apache.thrift/libthrift/0.10.0.jar
           |@maven//:org.slf4j/slf4j-api/1.7.12.jar""".stripMargin,
      allJars("@maven//:httpclient") ->
        """|@maven//:commons-codec/commons-codec/1.9.jar
           |@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apache.httpcomponents/httpclient/4.4.1.jar
           |@maven//:org.apache.httpcomponents/httpcore/4.4.1.jar""".stripMargin
    )
  )

  checkMultipleDeps(
    "replacements are respected",
    deps(
      dep("org.apache.thrift:libthrift:0.10.0")
        .target("libthrift")
        .exclude("commons-codec:commons-codec")
        .dependency("commons-codec-1-10"),
      dep("commons-codec:commons-codec:1.10")
        .target("commons-codec-1-10"),
      dep("org.apache.httpcomponents:httpclient:4.4.1")
        .target("httpclient")
    ),
    queries = List(
      allJars("@maven//:libthrift") ->
        """|@maven//:commons-codec/commons-codec/1.10.jar
           |@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apache.httpcomponents/httpclient/4.4.1.jar
           |@maven//:org.apache.httpcomponents/httpcore/4.4.1.jar
           |@maven//:org.apache.thrift/libthrift/0.10.0.jar
           |@maven//:org.slf4j/slf4j-api/1.7.12.jar""".stripMargin,
      allJars("@maven//:httpclient") ->
        """|@maven//:commons-codec/commons-codec/1.10.jar
           |@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apache.httpcomponents/httpclient/4.4.1.jar
           |@maven//:org.apache.httpcomponents/httpcore/4.4.1.jar""".stripMargin
    )
  )

  checkMultipleDeps(
    "additions are respected and don't leak to other resolutions",
    deps(
      dep("org.apiguardian:apiguardian-api:1.1.1")
        .target("apiguardian"),
      dep("commons-codec:commons-codec:1.9")
        .target("commons-codec")
        .dependency("apiguardian"),
      dep("org.apache.thrift:libthrift:0.10.0")
        .target("libthrift")
    ),
    queries = List(
      allJars("@maven//:apiguardian") ->
        """|@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin,
      allJars("@maven//:libthrift") ->
        """|@maven//:commons-codec/commons-codec/1.9.jar
           |@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apache.httpcomponents/httpclient/4.4.1.jar
           |@maven//:org.apache.httpcomponents/httpcore/4.4.1.jar
           |@maven//:org.apache.thrift/libthrift/0.10.0.jar
           |@maven//:org.slf4j/slf4j-api/1.7.12.jar""".stripMargin,
      allJars("@maven//:commons-codec") ->
        """|@maven//:commons-codec/commons-codec/1.9.jar
           |@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin
    )
  )

  checkMultipleDeps(
    "eviction keeps target information",
    deps(
      dep("org.apiguardian:apiguardian-api:1.1.1")
        .target("apiguardian"),
      dep("org.apiguardian:apiguardian-api:1.1.0")
        .target("apiguardian-old")
    ),
    arguments = exportCommand :+ "--no-fail-on-evicted-declared",
    queries = List(
      allJars("@maven//:apiguardian") ->
        """|@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin,
      allJars("@maven//:apiguardian-old") ->
        """|@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin
    ),
    expectedOutput =
      """|/workingDirectory/3rdparty.yaml:14:16 warning: Declared third party dependency 'org.apiguardian:apiguardian-api:1.1.0' is evicted in favor of 'org.apiguardian:apiguardian-api:1.1.1'.
         |Update the third party declaration to use version '1.1.1' instead of '1.1.0' to reflect the effective dependency graph.
         |Info:
         |  'org.apiguardian:apiguardian-api:1.1.0' is declared in apiguardian-old.
         |  'org.apiguardian:apiguardian-api:1.1.1' is a transitive dependency of apiguardian.
         |  - dependency: org.apiguardian:apiguardian-api:1.1.0
         |                ^
         |warning: 1 declared dependency was evicted.
         |""".stripMargin + defaultExpectedOutput
  )

  checkMultipleDeps(
    "multi-jar dependency and eviction",
    deps(
      dep("org.apiguardian:apiguardian-api:1.1.1")
        .target("multi-jar"),
      dep("commons-logging:commons-logging:1.2")
        .target("multi-jar"),
      dep("org.apiguardian:apiguardian-api:1.1.0")
        .target("apiguardian-old")
    ),
    arguments = exportCommand :+ "--no-fail-on-evicted-declared",
    queries = List(
      allJars("@maven//:multi-jar") ->
        """|@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin,
      allJars("@maven//:apiguardian-old") ->
        """|@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin
    ),
    expectedOutput =
      """|/workingDirectory/3rdparty.yaml:25:16 warning: Declared third party dependency 'org.apiguardian:apiguardian-api:1.1.0' is evicted in favor of 'org.apiguardian:apiguardian-api:1.1.1'.
         |Update the third party declaration to use version '1.1.1' instead of '1.1.0' to reflect the effective dependency graph.
         |Info:
         |  'org.apiguardian:apiguardian-api:1.1.0' is declared in apiguardian-old.
         |  'org.apiguardian:apiguardian-api:1.1.1' is a transitive dependency of multi-jar.
         |  - dependency: org.apiguardian:apiguardian-api:1.1.0
         |                ^
         |warning: 1 declared dependency was evicted.
         |""".stripMargin + defaultExpectedOutput
  )

  checkMultipleDeps(
    "multi-jar and multi-target",
    deps(
      dep("org.apiguardian:apiguardian-api:1.1.1")
        .target("apiguardian-and-commons-logging")
        .target("apiguardian-and-commons-codec"),
      dep("commons-logging:commons-logging:1.2")
        .target("apiguardian-and-commons-logging")
        .target("commons-logging-and-commons-codec"),
      dep("commons-codec:commons-codec:1.9")
        .target("apiguardian-and-commons-codec")
        .target("commons-logging-and-commons-codec")
    ),
    queries = List(
      allJars("@maven//:apiguardian-and-commons-logging") ->
        """|@maven//:commons-logging/commons-logging/1.2.jar
           |@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin,
      allJars("@maven//:apiguardian-and-commons-codec") ->
        """|@maven//:commons-codec/commons-codec/1.9.jar
           |@maven//:org.apiguardian/apiguardian-api/1.1.1.jar""".stripMargin,
      allJars("@maven//:commons-logging-and-commons-codec") ->
        """|@maven//:commons-codec/commons-codec/1.9.jar
           |@maven//:commons-logging/commons-logging/1.2.jar""".stripMargin
    ),
    expectedManifests = Map(
      "bazel-deps-apiguardian-and-commons-logging.txt" ->
        """|commons-logging:commons-logging:1.2
           |org.apiguardian:apiguardian-api:1.1.1""".stripMargin,
      "bazel-deps-apiguardian-and-commons-codec.txt" ->
        """|commons-codec:commons-codec:1.9
           |org.apiguardian:apiguardian-api:1.1.1""".stripMargin,
      "bazel-deps-commons-logging-and-commons-codec.txt" ->
        """|commons-codec:commons-codec:1.9
           |commons-logging:commons-logging:1.2""".stripMargin
    )
  )

  checkMultipleDeps(
    "Generate target for inexistent classifiers",
    deps(
      dep("io.zipkin:zipkin-thrift:1.4.1")
        .classifier("whatever")
        .target("with-classifier")
    ),
    queries = List(
      allJars("@maven//:with-classifier") ->
        "@maven//:org.scala-lang/scala-library/2.11.7.jar"
    )
  )

  checkMultipleDeps(
    "different exclusions and eviction",
    deps(
      dep("org.apache.parquet:parquet-thrift:1.9.0")
        .target("3rdparty/jvm/org/apache/parquet:parquet-thrift-1.9.0")
        .exclude("com.twitter.elephantbird:*"),
      dep("org.apache.parquet:parquet-thrift:1.11.0")
        .target("3rdparty/jvm/org/apache/parquet:parquet-thrift-1.11.0")
        .exclude("com.hadoop.gplcompression:hadoop-lzo")
    ),
    arguments = exportCommand :+ "--no-fail-on-evicted-declared",
    queries = List(
      allJars("@maven//:3rdparty/jvm/org/apache/parquet_parquet-thrift-1.11.0") ->
        s"""|@maven//:com.google.code.findbugs/jsr305/1.3.9.jar
            |@maven//:com.google.guava/guava/11.0.1.jar
            |@maven//:com.google.protobuf/protobuf-java/2.4.1.jar
            |@maven//:com.googlecode.json-simple/json-simple/1.1.jar
            |@maven//:com.twitter.elephantbird/elephant-bird-core/4.4.jar
            |@maven//:com.twitter.elephantbird/elephant-bird-hadoop-compat/4.4.jar
            |@maven//:com.twitter.elephantbird/elephant-bird-pig/4.4.jar
            |@maven//:commons-codec/commons-codec/1.3.jar
            |@maven//:commons-lang/commons-lang/2.5.jar
            |@maven//:commons-logging/commons-logging/1.1.1.jar
            |@maven//:commons-pool/commons-pool/1.6.jar
            |@maven//:javax.annotation/javax.annotation-api/1.3.2.jar
            |@maven//:javax.servlet/servlet-api/2.5.jar
            |@maven//:org.apache.httpcomponents/httpclient/4.0.1.jar
            |@maven//:org.apache.httpcomponents/httpcore/4.0.1.jar
            |@maven//:org.apache.parquet/parquet-column/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-common/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-encoding/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-format-structures/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-hadoop/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-jackson/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-pig/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-thrift/1.11.0.jar
            |@maven//:org.apache.thrift/libthrift/0.7.0.jar
            |@maven//:org.apache.yetus/audience-annotations/0.11.0.jar
            |@maven//:org.slf4j/slf4j-api/1.7.22.jar
            |@maven//:org.xerial.snappy/snappy-java/1.1.7.3.jar
            |""".stripMargin,
      allJars("@maven//:3rdparty/jvm/org/apache/parquet_parquet-thrift-1.9.0") ->
        s"""|@maven//:commons-pool/commons-pool/1.6.jar
            |@maven//:javax.annotation/javax.annotation-api/1.3.2.jar
            |@maven//:org.apache.parquet/parquet-column/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-common/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-encoding/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-format-structures/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-hadoop/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-jackson/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-pig/1.11.0.jar
            |@maven//:org.apache.parquet/parquet-thrift/1.11.0.jar
            |@maven//:org.apache.yetus/audience-annotations/0.11.0.jar
            |@maven//:org.slf4j/slf4j-api/1.7.22.jar
            |@maven//:org.xerial.snappy/snappy-java/1.1.7.3.jar
            |""".stripMargin
    ),
    expectedOutput =
      """|/workingDirectory/3rdparty.yaml:3:16 warning: Declared third party dependency 'org.apache.parquet:parquet-thrift:1.9.0' is evicted in favor of 'org.apache.parquet:parquet-thrift:1.11.0'.
         |Update the third party declaration to use version '1.11.0' instead of '1.9.0' to reflect the effective dependency graph.
         |Info:
         |  'org.apache.parquet:parquet-thrift:1.9.0' is declared in parquet-thrift-1.9.0.
         |  'org.apache.parquet:parquet-thrift:1.11.0' is a transitive dependency of parquet-thrift-1.11.0.
         |  - dependency: org.apache.parquet:parquet-thrift:1.9.0
         |                ^
         |warning: 1 declared dependency was evicted.
         |""".stripMargin + defaultExpectedOutput,
    expectedManifests = Map(
      "3rdparty/jvm/org/apache/parquet/bazel-deps-parquet-thrift-1.11.0.txt" ->
        """|com.google.code.findbugs:jsr305:1.3.9
           |com.google.guava:guava:11.0.1
           |com.google.protobuf:protobuf-java:2.4.1
           |com.googlecode.json-simple:json-simple:1.1
           |com.twitter.elephantbird:elephant-bird-core:4.4
           |com.twitter.elephantbird:elephant-bird-hadoop-compat:4.4
           |com.twitter.elephantbird:elephant-bird-pig:4.4
           |commons-codec:commons-codec:1.3
           |commons-lang:commons-lang:2.5
           |commons-logging:commons-logging:1.1.1
           |commons-pool:commons-pool:1.6
           |javax.annotation:javax.annotation-api:1.3.2
           |javax.servlet:servlet-api:2.5
           |org.apache.httpcomponents:httpclient:4.0.1
           |org.apache.httpcomponents:httpcore:4.0.1
           |org.apache.parquet:parquet-column:1.11.0
           |org.apache.parquet:parquet-common:1.11.0
           |org.apache.parquet:parquet-encoding:1.11.0
           |org.apache.parquet:parquet-format-structures:1.11.0
           |org.apache.parquet:parquet-hadoop:1.11.0
           |org.apache.parquet:parquet-jackson:1.11.0
           |org.apache.parquet:parquet-pig:1.11.0
           |org.apache.parquet:parquet-thrift:1.11.0
           |org.apache.thrift:libthrift:0.7.0
           |org.apache.yetus:audience-annotations:0.11.0
           |org.slf4j:slf4j-api:1.7.22
           |org.xerial.snappy:snappy-java:1.1.7.3""".stripMargin,
      "3rdparty/jvm/org/apache/parquet/bazel-deps-parquet-thrift-1.9.0.txt" ->
        """|commons-pool:commons-pool:1.6
           |javax.annotation:javax.annotation-api:1.3.2
           |org.apache.parquet:parquet-column:1.11.0
           |org.apache.parquet:parquet-common:1.11.0
           |org.apache.parquet:parquet-encoding:1.11.0
           |org.apache.parquet:parquet-format-structures:1.11.0
           |org.apache.parquet:parquet-hadoop:1.11.0
           |org.apache.parquet:parquet-jackson:1.11.0
           |org.apache.parquet:parquet-pig:1.11.0
           |org.apache.parquet:parquet-thrift:1.11.0
           |org.apache.yetus:audience-annotations:0.11.0
           |org.slf4j:slf4j-api:1.7.22
           |org.xerial.snappy:snappy-java:1.1.7.3""".stripMargin
    )
  )

  checkMultipleDeps(
    "round-trip dependency",
    deps(
      dep("org.apache.thrift:libthrift:0.10.0")
        .target("libthrift"),
    ) ++ overrideTargets("org.apache.httpcomponents:httpclient" -> "@//foo:bar"),
    extraBuild = """|/foo/BUILD
                    |load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_binary")
                    |
                    |scala_library(
                    |  name = "bar",
                    |  srcs = [],
                    |)""".stripMargin,
    queries = List(
      allScalaLibDeps("@maven//:libthrift") ->
        """|//foo:bar""".stripMargin,
      allJars("@maven//:libthrift") ->
        """|@maven//:org.apache.httpcomponents/httpcore/4.4.1.jar
           |@maven//:org.apache.thrift/libthrift/0.10.0.jar
           |@maven//:org.slf4j/slf4j-api/1.7.12.jar""".stripMargin,
    ),
    expectedManifests = Map(
      "bazel-deps-libthrift.txt" ->
        """|org.apache.httpcomponents:httpcore:4.4.1
           |org.apache.thrift:libthrift:0.10.0
           |org.slf4j:slf4j-api:1.7.12""".stripMargin,
    )
  )

  checkMultipleDeps(
    "round-trip dependency with dependencies",
    deps(
      dep("org.apache.thrift:libthrift:0.10.0")
        .target("libthrift"),
      dep("org.apiguardian:apiguardian-api:1.1.1")
        .target("apiguardian")
    ) ++ overrideTargets("org.apache.httpcomponents:httpclient" -> "@//foo:bar"),
    extraBuild = """|/foo/BUILD
                    |load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_binary")
                    |
                    |scala_library(
                    |  name = "bar",
                    |  srcs = [],
                    |  deps = ["@maven//:apiguardian"]
                    |)""".stripMargin,
    queries = List(
      allScalaLibDeps("@maven//:libthrift") ->
        """|//foo:bar""".stripMargin,
      allJars("@maven//:libthrift") ->
        """|@maven//:org.apache.httpcomponents/httpcore/4.4.1.jar
           |@maven//:org.apache.thrift/libthrift/0.10.0.jar
           |@maven//:org.apiguardian/apiguardian-api/1.1.1.jar
           |@maven//:org.slf4j/slf4j-api/1.7.12.jar""".stripMargin,
    ),
    expectedManifests = Map(
      "bazel-deps-libthrift.txt" ->
        """|org.apache.httpcomponents:httpcore:4.4.1
           |org.apache.thrift:libthrift:0.10.0
           |org.slf4j:slf4j-api:1.7.12""".stripMargin,
      "bazel-deps-apiguardian.txt" ->
        """|org.apiguardian:apiguardian-api:1.1.1""".stripMargin
    )
  )

  checkMultipleDeps(
    "intra-target conflict",
    deps(
      dep("com.google.auto:auto-common:1.0")
        .target("broken-target"),
      dep("com.google.inject:guice:4.0")
        .target("broken-target")
    ),
    expectedExit = 1,
    expectedOutput =
      """|/workingDirectory/3rdparty.yaml:14:16 error: Within 'broken-target', the module 'com.google.guava:guava' is resolved multiple times with incompatible versions 16.0.1, 30.1.1-jre.
         |To fix this problem, update your dependencies to compatible versions, or add exclusion rules to force compatible versions of 'com.google.guava:guava'.
         |
         |  - dependency: com.google.inject:guice:4.0
         |                ^""".stripMargin
  )

  checkDeps(
    "direct force wins over higher transitive dependencies",
    deps(
      dep("com.google.cloud:pubsublite-kafka:0.1.1")
        .target("pubsublite-kafka")
        .force(true),
      dep("com.fasterxml.jackson.core:jackson-core:2.11.0")
        .target("jackson-core")
        .force(true)
    )
  )

  checkDeps(
    "don't report evicted declared unforced dependencies",
    deps(
      dep("org.apache.thrift:libthrift:0.10.0")
        .target("libthrift"),
      dep("org.slf4j:slf4j-api:1.7.10")
        .target("slf4j")
        .force(false)
    )
  )

  checkMultipleDeps(
    "reconciliation with version extractor",
    deps(
      dep("com.google.apis:google-api-services-storage:v1-rev20190624-1.30.1")
        .target("storage-1.30.1")
        .transitive(false)
        .versionPattern("-([0-9\\.]+)$"),
      dep("com.google.apis:google-api-services-storage:v1-rev20200326-1.30.9")
        .target("storage-1.30.9")
        .transitive(false),
    ),
    arguments = exportCommand :+ "--no-fail-on-evicted-declared",
    expectedExit = 0,
    queries = List(
      allGenrules ->
        """|@maven//:genrules/com.google.apis_google-api-services-storage_v1-rev20200326-1.30.9
           |""".stripMargin
    ),
    expectedOutput =
      """|/workingDirectory/3rdparty.yaml:3:16 warning: Declared third party dependency 'com.google.apis:google-api-services-storage:v1-rev20190624-1.30.1' is evicted in favor of 'com.google.apis:google-api-services-storage:v1-rev20200326-1.30.9'.
         |Update the third party declaration to use version 'v1-rev20200326-1.30.9' instead of 'v1-rev20190624-1.30.1' to reflect the effective dependency graph.
         |Info:
         |  'com.google.apis:google-api-services-storage:v1-rev20190624-1.30.1' is declared in storage-1.30.1.
         |  'com.google.apis:google-api-services-storage:v1-rev20200326-1.30.9' is a transitive dependency of storage-1.30.9.
         |  - dependency: com.google.apis:google-api-services-storage:v1-rev20190624-1.30.1
         |                ^
         |warning: 1 declared dependency was evicted.
         |""".stripMargin + defaultExpectedOutput
  )
}
