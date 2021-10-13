package home.yuranich.genomeanalysis.spark;

import htsjdk.samtools.ValidationStringency;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.rdd.RDD;
import org.assertj.core.api.Assertions;
import org.bdgenomics.adam.api.java.JavaADAMContext;
import org.bdgenomics.adam.ds.ADAMContext;
import org.bdgenomics.adam.ds.read.AlignmentDataset;
import org.bdgenomics.adam.ds.sequence.SliceDataset;
import org.bdgenomics.adam.ds.variant.GenotypeDataset;
import org.bdgenomics.adam.models.ReferenceRegion;
import org.bdgenomics.formats.avro.Strand;
import org.junit.Test;
import scala.Function1;
import scala.Tuple2;
import scala.reflect.ClassTag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class KMersCountTest {
    private SparkConf sparkConf = new SparkConf(false)
            .setAppName("testADAM" + ": " + "sparkName")
            .setMaster("local[15]")
            .set("spark.driver.memory", "32g")
            .set("spark.driver.port", "50030")
            .set("spark.ui.enabled", "false")
            .set("spark.driver.allowMultipleContexts", "true");

    private SparkContext sc = SparkContext.getOrCreate(sparkConf);

    private JavaADAMContext jac = new JavaADAMContext(new ADAMContext(sc));

    @Test(expected = FileNotFoundException.class)
    public void testLoadEmptyDirectory() throws IOException {
        String empty = Files.createTempDirectory("").toAbsolutePath().toString();

        jac.loadAlignments(empty);
    }

    @Test
    public void canReadSmallVcfFileAsGenotype() {
        URL small = ClassLoader.getSystemResource("small.vcf");
        GenotypeDataset genotypeDataset = jac.loadGenotypes(small.getFile());

        Assertions.assertThat(genotypeDataset.jrdd().count()).isEqualTo(18);
    }

    @Test
    public void countKmersTestExample() {
        ReferenceRegion refRegion = new ReferenceRegion("1", 1, 10, Strand.INDEPENDENT);
        URL small = ClassLoader.getSystemResource("sorted.2.bam");
        AlignmentDataset alignments = jac.loadIndexedBam(small.getFile(), Collections.singletonList(refRegion), ValidationStringency.STRICT);

        RDD<Tuple2<String, Object>> kmers = alignments.countKmers(3);

        System.out.println("Kmers count: " + kmers.count());

        kmers.map(new TupleToString(), ClassTag.apply(String.class)).saveAsTextFile("kmers");
    }

    @Test
    public void countKmersBiggerSampleTest() {
//        ReferenceRegion refRegion = new ReferenceRegion("1", 1, 10, Strand.INDEPENDENT);
        URL chr11 = ClassLoader.getSystemResource("chr11.fa");
        SliceDataset slices = jac.loadSlices(chr11.getFile(), 1000);
        System.out.println(slices.jrdd().count());
        System.out.println(slices.jrdd().first());
        System.out.println(slices.jrdd().take(5));

        RDD<Tuple2<String, Object>> kmers = slices.countKmers(3);

        System.out.println("Kmers count: " + kmers.count());

        kmers.map(new TupleToString(), ClassTag.apply(String.class)).saveAsTextFile("kmers2");
    }

    @Test
    public void countKmersFULLSample_5gb() {
        URL chr11 = ClassLoader.getSystemResource("ERR047879_1.fastq");
        AlignmentDataset alignments = jac.loadAlignments(chr11.getFile());

        System.out.println(alignments.rdd().count());

        RDD<Tuple2<String, Object>> kmers = alignments.countKmers(21);

        System.out.println("Kmers count: " + kmers.count());

        List<String> result = kmers.toJavaRDD()
                .sortBy(Tuple2::_2, false, kmers.getNumPartitions()).map(v1 -> v1._1() + ": " + v1._2)
                .take(20);
        System.out.println(result);
    }

    public static class TupleToString implements Function1<Tuple2<String, Object>, String>, Serializable {
        @Override
        public String apply(Tuple2<String, Object> v1) {
            return v1._1() + ": " + v1._2;
        }
    }
}
