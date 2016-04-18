import java.util.Arrays;

/**
 * Created by oleg on 03.10.15.
 */
public class MatrixOperations {

    /**
     * Генерує матрицю, заповнює одиницями
     *
     * @param n розмірність
     */
    public static int[][] inputMatrix(int n) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                result[i][j] = 1;
            }
        return result;
    }

    /**
     * Генерує вектор, заповнює одиницями
     *
     * @param n розмірність
     */
    public static int[] inputVector(int n) {
        int[] result = new int[n];
        int start = 4;
        for (int i = 0; i < result.length; i++) {
            result[i] = start + i;
        }
        return result;
    }

    public static int inputConstant() {
        return 1;
    }

    /**
     * Додає матриці
     *
     * @param param1 матриця 1
     * @param param2 матриця 2
     * @return сума матриць
     */
    public static int[][] addMatrix(int[][] param1, int[][] param2, int const1, int const2) {
        if (param1.length != param2.length) {
            System.out.println("Нельзя суммировать матрицы с разным количеством строк");
            return null;
        }
        int[][] result = new int[param1.length][param1[0].length];
        for (int i = 0; i < result.length; i++) {
            result[i] = addVectors(param1[i], param2[i], const1, const2);
        }
        System.out.println("result = " + result);
        return result;
    }

    /**
     * Додає вектори
     *
     * @param p1 вектор1
     * @param p2 вектор2
     * @return сума векторів
     */
    public static int[] addVectors(int[] p1, int[] p2, int const1, int const2) {
        int[] result = new int[p1.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = p1[i] * const1 + p2[i] * const2;
        }
        return result;
    }

    /**
     * Перемножає матриці
     *
     * @param param1 матриця-множник
     * @param param2 матриця-множене
     * @return добуток матриць, розмірність - nxn
     */
    public static int[][] multMatrix(int[][] param1, int[][] param2) {
        System.out.println("param1 = " + formattedDeepToString(param1));
        System.out.println("param2 = " + formattedDeepToString(param2));
        if (param1[0].length != param2.length) {
            System.out.println("Нельзя умножать матрицы, количество элементов в строке которой не равно кол-ву столбцов в другой");
            return null;
        }
        int[][] result = new int[param1.length][param1[0].length];
        for (int k = 0; k < param1.length; k++) {
            for (int i = 0; i < param1[0].length; i++) {
                for (int j = 0; j < param2.length; j++) {
                    result[k][i] += param1[k][j] * param2[j][i]; //TODO: перепроверить правильно ли считает - вроде норм!
                }
            }
        }
        return result;
    }

    public static String formattedDeepToString(int[][] matrix) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            result.append(Arrays.toString(matrix[i])).append("\n");
        }
        return result.toString();
    }

    public static int min(int[] vector, int startIndex, int endIndex) {
        int result = vector[startIndex];
        for (int i = startIndex + 1; i < endIndex; i++) {
            result = result > vector[i] ? vector[i] : result;
        }
        return result;
    }
}