package home.yuranich.genomeanalysis.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.assertj.core.api.Assertions;
import org.bdgenomics.adam.api.java.JavaADAMContext;
import org.bdgenomics.adam.ds.ADAMContext;
import org.bdgenomics.adam.ds.variant.GenotypeDataset;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class ADAMContextTest {
    private SparkConf sparkConf = new SparkConf(false)
            .setAppName("testADAM" + ": " + "sparkName")
            .setMaster("local[4]")
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
}
