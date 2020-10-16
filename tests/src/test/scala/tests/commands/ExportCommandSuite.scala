package tests.commands

class ExportCommandSuite extends tests.BaseSuite {

  test("conflict") {
    checkCommand(
      arguments = List("export", "--no-use-ansi-output"),
      expectedExit = 1,
      expectedOutput =
        """|info: generated: /workingDirectory/3rdparty/jvm_deps.bzl
           |""".stripMargin,
      workingDirectoryLayout = s"""|/3rdparty.yaml
                                   |scala: 2.12.12
                                   |dependencies:
                                   |  - dependency: com.google.guava:guava:29.0-jre
                                   |  - dependency: org.eclipse.lsp4j:org.eclipse.lsp4j:0.9.0
                                   |$bazelWorkspace
                                   |""".stripMargin
    )
  }
}
