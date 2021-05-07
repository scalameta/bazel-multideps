package multiversion.outputs

import java.nio.file.Files
import java.nio.file.Path

import multiversion.diagnostics.MultidepsEnrichments.XtensionDependency
import multiversion.diagnostics.MultidepsEnrichments.XtensionList
import org.typelevel.paiges.Doc

final case class DepsOutput(
    artifacts: Seq[ArtifactOutput],
    index: ResolutionIndex,
    outputIndex: collection.Map[String, ArtifactOutput]
) {
  require(artifacts.nonEmpty)

  val allTargets: Set[String] = index.thirdparty.dependencies2.flatMap(_.targets).toSet

  def render: String = {
    val width = 120000

    val httpFiles = Doc
      .intercalate(Doc.line, artifacts.map(_.httpFile.toDoc))
      .nested(4)
      .render(width)
    val thirdPartyImports = Doc
      .intercalate(
        Docs.blankLine,
        allTargets.map(ArtifactOutput.buildThirdPartyDoc(_, index, outputIndex))
      )
      .render(width)
    val builds = Doc
      .intercalate(
        Docs.blankLine,
        artifacts.map(ArtifactOutput.buildDoc(_, index, outputIndex))
      )
      .render(width)
    val evictedBuilds = Doc
      .intercalate(
        Docs.blankLine,
        index.evictionPairs.toList
          .sortBy(_._1.bazelLabel)
          .distinctBy(_._1.bazelLabel)
          .map {
            case (d, w) =>
              ArtifactOutput.buildEvictedDoc(d, w, index, outputIndex)
          }
      )
      .render(width)
    s"""# DO NOT EDIT: this file is auto-generated
def _jvm_deps_impl(ctx):
    content = \"\"\"
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

def load_jvm_deps():
    $httpFiles

\"\"\"
    ctx.file("jvm_deps.bzl", content, executable=False)
    build_content = \"\"\"
load(\"@io_bazel_rules_scala//scala:scala_import.bzl\", \"scala_import\")

$thirdPartyImports

$builds

$evictedBuilds
\"\"\"
    ctx.file("BUILD", build_content, executable=False)


jvm_deps_rule = repository_rule(
    implementation=_jvm_deps_impl,
)


def jvm_deps():
    jvm_deps_rule(name="maven")
""".stripMargin
  }

  def writeManifests(root: Path): Unit =
    allTargets.foreach { target =>
      val manifest = manifestPath(root, target)
      val dependencies = for {
        config <- index.thirdparty.depsByTargets(target)
        dependency <- index.dependencies.getOrElse(config.id, Nil)
      } yield index.reconciledDependency(dependency).repr
      val output = dependencies.distinct.sorted.mkString(System.lineSeparator())
      Files.createDirectories(manifest.getParent())
      Files.write(manifest, output.getBytes)
    }

  private def manifestPath(root: Path, target: String): Path = {
    val cleanTarget = target.replaceAll("^/*", "")
    val (path, name) = cleanTarget.splitAt(cleanTarget.indexOf(':'))
    root.resolve(path).resolve(s"bazel-deps-${name.stripPrefix(":")}.txt")
  }
}
