import java.util.Arrays;

/**
 * Created by Oleh Kakherskyi, student of the KPI, FICT, IP-31 group (olehkakherskiy@gmail.com) on 18.04.2016.
 */
public class DataPackBuilder {

    private static DataPack[] preparedPacks;

    public static int[] elementsCount;

    public static int[] dataOffset;

    public static void setMetadata(int processCount, int[] elemCount, int[] offset) {
        preparedPacks = new DataPack[processCount];
        for (int i = 0; i < preparedPacks.length; i++) {
            preparedPacks[i] = new DataPack();
        }
        elementsCount = elemCount;
        dataOffset = offset;
    }

    public static DataPack[] packData(int[][] matrix1, int[][] matrix2, int[] vector, int constant, boolean matrix1FullSize) {
        for (int i = 0; i < preparedPacks.length; i++) {
            if (matrix1FullSize)
                preparedPacks[i].setMatrix1(matrix1);
            else
                preparedPacks[i].setMatrix1(Arrays.copyOfRange(matrix1, dataOffset[i], dataOffset[i] + elementsCount[i]));

            preparedPacks[i].setMatrix2(Arrays.copyOfRange(matrix2, dataOffset[i], dataOffset[i] + elementsCount[i]));
            if (vector != null)
                preparedPacks[i].setVector(Arrays.copyOfRange(vector, dataOffset[i], dataOffset[i] + elementsCount[i]));
            preparedPacks[i].setConstant(constant);
        }
        return preparedPacks;
    }
}
