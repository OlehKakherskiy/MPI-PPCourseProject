import java.util.Arrays;

/**
 * Created by Oleh Kakherskyi, student of the KPI, FICT, IP-31 group (olehkakherskiy@gmail.com) on 16.04.2016.
 */
public class GraphTopology {

    /**
     * Возврат массива степеней каждой вершины. Так как у нас k-мерный гиперкуб, то степени всех вершин одинаковы
     *
     * @param k степень выхода каждой вершины
     * @param n количество вершин
     * @return
     */
    public static int[] calculateIndexes(int k, int n) {
        int[] result = new int[n];
        for (int i = 1; i <= result.length; i++) {
            result[i - 1] = k * i;
        }
        return result;
    }

    public static int[] calculateEdges(int k, int n) {
        int[][] buffer = new int[n][k];

        for (int[] aResult : buffer) {
            Arrays.fill(aResult, -1);
        }

        for (int i = 0; i < k; i++) {
            int neighbourBaseNumber = (int) Math.pow(2, i); //на сколько отличается номер соседей
            for (int j = 0; j < n; j++)
                if (buffer[j][i] == -1) {
                    buffer[j][i] = j + neighbourBaseNumber;
                    buffer[j + neighbourBaseNumber][i] = j;
                }
        }
        int[] result = new int[k * n];
        //формируем массив edge
        for (int i = 0; i < n; i++) {
            System.arraycopy(buffer[i], 0, result, k * i, k);
        }
//        System.out.println(Arrays.toString(result));
        return result;
    }

    public static void main(String[] args) {
        int k = 3;
        int n = (int) Math.pow(2, k);
        System.out.println(Arrays.toString(calculateIndexes(k, n)));
        System.out.println();
        calculateEdges(k, n);
    }
}
