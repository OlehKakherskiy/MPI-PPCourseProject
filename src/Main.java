import mpi.*;

import java.util.Arrays;

public class Main {

    private static int[][] MA, MB, MO, MC, MK;
    private static int[] Z;
    private static int a, minZ, N, H;
    private static int p;

    public static void main(String[] args) {

        MPI.Init(args);

        long startTime = System.currentTimeMillis();
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        System.out.println("Process " + rank + " is started");

        //определяем размерность матриц - передана как последний параметр командной строки
        N = Integer.parseInt(args[args.length - 1]);
        H = N / size;
        p = size - 1;

        int power = (int) (Math.log(size) / Math.log(2)); //степень каждой вершины

        //создаем топологию на основе глобального коммуникатора - каждый процесс будет знать своих соседей
        // (необходимо для передачи данных), таким образом реализовываем гиперкуб
        MPI.COMM_WORLD.Create_graph(GraphTopology.calculateIndexes(power, size), GraphTopology.calculateEdges(power, size), false);

        //определяем смещения
        int[] displacement = getEachOffset(size);

        //определяем количество элементов
        int[] indexCount = getEachCount(size);

        //ввод данных
        initData(rank, indexCount);

        //получение/отправка данных с последнего процесса
        MPI.COMM_WORLD.Scatterv(Z, 0, indexCount, displacement, MPI.INT, Z, 0, indexCount[rank], MPI.INT, size - 1);
        MPI.COMM_WORLD.Scatterv(MC, 0, indexCount, displacement, MPI.OBJECT, MC, 0, indexCount[rank], MPI.OBJECT, size - 1);
        MPI.COMM_WORLD.Scatterv(MK, 0, indexCount, displacement, MPI.OBJECT, MK, 0, indexCount[rank], MPI.OBJECT, size - 1);

        //получение/отправка данных с первого процесса
        int ar[] = {a};
        MPI.COMM_WORLD.Bcast(ar, 0, 1, MPI.INT, 0);
        a = ar[0];
        MPI.COMM_WORLD.Bcast(MB, 0, N, MPI.OBJECT, 0);
        MPI.COMM_WORLD.Scatterv(MO, 0, indexCount, displacement, MPI.OBJECT, MO, 0, indexCount[rank], MPI.OBJECT, 0);

        // обрезка элементов в векторах и строк в матрицах в процессах p и 0 - отсавляем только нужные для работы
        // (остальные расшарены по другим процессам)
        if (rank == p) {
            Z = Arrays.copyOfRange(Z, Z.length - H, Z.length);
            MC = MatrixOperations.truncateMatrix(MC, N - H, H);
            MK = MatrixOperations.truncateMatrix(MK, N - H, H);
        } else if (rank == 0) {
            MO = MatrixOperations.truncateMatrix(MO, 0, indexCount[0]);
        }

        //вычисление локальных минимумов
        int[] localMinimums = new int[1];
        localMinimums[0] = MatrixOperations.min(Z, 0, Z.length);

        //вычисление глобального минимума
        MPI.COMM_WORLD.Reduce(localMinimums, 0, localMinimums, 0, 1, MPI.INT, MPI.MIN, p);

        //расшаривание глобального минимума
        MPI.COMM_WORLD.Bcast(localMinimums, 0, 1, MPI.INT, p);
        minZ = localMinimums[0];

        //вычисление частичных результатов задания
        MA = MatrixOperations.addMatrix(
                MatrixOperations.multMatrix(MatrixOperations.addMatrix(MC, MO, 1, 1), MB), MK, a, minZ);

        //сборка частичных результатов мат. выражения со всех процессов в процесс 0
        int[][] result = new int[N][];
        MPI.COMM_WORLD.Gatherv(MA, 0, indexCount[rank], MPI.OBJECT, result, 0, indexCount, displacement, MPI.OBJECT, 0);

        //вывод результатов на экран и завершение работы
        if (rank == 0) {
            if (result.length <= 16)
                System.out.println(MatrixOperations.formattedDeepToString(result));
            System.out.println("Computation time (sec): " + ((System.currentTimeMillis() - startTime) / 1000.0));
        }
        System.out.println("Process " + rank + " is finished");
        MPI.Finalize();
    }

    //TODO: 3. упаковка/распаковка

    private static void initData(int rank, int[] indexCount) {
        if (rank == 0) {
            MB = MatrixOperations.inputMatrix(N);
            MO = MatrixOperations.inputMatrix(N);
            a = MatrixOperations.inputConstant();
            MC = new int[indexCount[rank]][N];
            MK = new int[indexCount[rank]][N];
            Z = new int[indexCount[rank]];

        } else if (rank == p) {
            MC = MatrixOperations.inputMatrix(N);
            MK = MatrixOperations.inputMatrix(N);
            Z = MatrixOperations.inputVector(N);
            MB = new int[N][N];
            MO = new int[H][N];
        } else {
            MB = new int[N][N];
            MO = new int[H][N];
            MC = new int[H][N];
            MK = new int[H][N];
            Z = new int[H];
        }
    }

    /**
     * Вычисление количества элементов в векторах и кол-во строк в матрицах, которые будут переданы каждому процессу
     * для вычисления мат. выражения
     *
     * @param processCount количество процессов
     * @return количество элементов
     */
    private static int[] getEachCount(int processCount) {
        int[] result = new int[processCount];
        Arrays.fill(result, 1, result.length, H);
        //первый процесс получит больше элементов, если их неравное количество на каждый процесс
        result[0] = N - (result.length - 1) * H;
        return result;
    }

    /**
     * Вычисление смещения в векторах и матрицах, начиная с которого будет передано {@link #getEachCount(int)}(rank)
     * элементов векторов и строк матриц процессу с номером rank
     *
     * @param processCount количество процессов
     * @return смещение в векторах и матрицах
     */
    private static int[] getEachOffset(int processCount) {
        int[] result = new int[processCount];
        result[0] = 0;
        result[1] = N - (result.length - 1) * H;
        for (int i = 2; i < result.length; i++) {
            result[i] = result[i - 1] + H;
        }
        return result;
    }
}