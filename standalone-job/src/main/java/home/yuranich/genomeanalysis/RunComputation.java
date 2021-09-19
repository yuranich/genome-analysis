package home.yuranich.genomeanalysis;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.SparkSession;
import org.bdgenomics.adam.api.java.JavaADAMContext;
import org.bdgenomics.adam.ds.ADAMContext;
import org.bdgenomics.adam.ds.read.AlignmentDataset;
import scala.Tuple2;

import java.util.List;

public class RunComputation {
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Please specify only the path to exome file.");
        }

        SparkContext sc = SparkSession.builder().appName("testADAMgenome").getOrCreate().sparkContext();

        JavaADAMContext jac = new JavaADAMContext(new ADAMContext(sc));

        AlignmentDataset alignments = jac.loadAlignments(args[0]);

        System.out.println(alignments.rdd().count());

        RDD<Tuple2<String, Object>> kmers = alignments.countKmers(21);

        System.out.println("Kmers count: " + kmers.count());

        List<String> result = kmers.toJavaRDD()
                .sortBy(Tuple2::_2, false, kmers.getNumPartitions()).map(v1 -> v1._1() + ": " + v1._2)
                .take(20);
        System.out.println(result);
    }
}
