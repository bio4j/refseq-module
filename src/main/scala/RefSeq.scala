package ohnosequences.bio4j.bundles

import shapeless._
import shapeless.ops.hlist._
import ohnosequences.typesets._
import ohnosequences.statika._
import ohnosequences.statika.aws._
import ohnosequences.statika.ami._
import ohnosequences.bio4j.statika._
import ohnosequences.awstools.s3._
import ohnosequences.awstools.regions._
import com.ohnosequences.bio4j.titan.programs._
import java.io._


case object RefSeqRawData extends RawDataBundle("ftp://ftp.ncbi.nih.gov/refseq/release/complete/*.gbff.gz") {
  override val dataFolder = new java.io.File("refseq_data")

  override def install[D <: AnyDistribution](d: D): InstallResults = {
    if (!dataFolder.exists) dataFolder.mkdirs

    Seq("wget", url) @@ dataFolder -&-
    "gunzip *.gz" @@ dataFolder ->-
    success(s"${url} is downloaded and unpacked to ${dataFolder}")
  }
}

case object RefSeqAPI extends APIBundle(){}

case class RefSeqProgram(
  dataFolder : File, // 1. Folder name with all the .gbk files
  db         : File  // 2. Bio4j DB folder
) extends ImporterProgram(new ImportRefSeqTitan(), Seq(
  dataFolder.getAbsolutePath, 
  db.getAbsolutePath
))

case object RefSeqImportedData extends ImportedDataBundle(
    rawData = RefSeqRawData :~: ∅,
    initDB = InitialBio4j
  ) {
  override def install[D <: AnyDistribution](d: D): InstallResults = {
    RefSeqProgram(
      dataFolder = RefSeqRawData.dataFolder,
      db         = dbLocation
    ).execute ->-
    success(s"Data ${name} is imported to ${dbLocation}")
  }
}

case object RefSeqModule extends ModuleBundle(RefSeqAPI, RefSeqImportedData)

case object RefSeqMetadata extends generated.metadata.RefseqModule()

case object RefSeqRelease extends ReleaseBundle(
  ObjectAddress("bio4j.releases", 
                "refseq/v" + RefSeqMetadata.version.stripSuffix("-SNAPSHOT")), 
  RefSeqModule
)

case object RefSeqDistribution extends DistributionBundle(
  RefSeqRelease,
  destPrefix = new File("/media/ephemeral0/")
)

