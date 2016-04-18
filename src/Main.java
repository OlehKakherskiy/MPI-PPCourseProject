import mpi.MPI;

import java.util.Arrays;

public class Main {

    private static int[][] MA, MB, MO, MC, MK;
    private static int[] Z;
    public static int a, minZ, N, H;
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

        //ввод данных
        initData(rank);

        int[] indexCount = getEachCount(size);
        int[] displacement = getEachOffset(size);

        if (rank == 0 || rank == p) {
            //устанавливаем метаданные для упаковки данных
            DataPackBuilder.setMetadata(size, getEachCount(size), getEachOffset(size));
        }

        DataPack[] packs = rank == p ? DataPackBuilder.packData(MC, MK, Z, 0, false) : new DataPack[1];
        int[] packIndexCount = new int[size];
        Arrays.fill(packIndexCount, 1);
        int[] packDisplacement = new int[size];
        for (int i = 1; i < packDisplacement.length; i++) {
            packDisplacement[i] = packDisplacement[i - 1] + 1;
        }
        //получение/отправка данных с последнего процесса
        MPI.COMM_WORLD.Scatterv(packs, 0, packIndexCount, packDisplacement, MPI.OBJECT, packs, 0, packIndexCount[rank], MPI.OBJECT, size - 1);

        //получение/отправка данных с первого процесса
        DataPack[] packs1 = rank == 0 ? DataPackBuilder.packData(MB, MO, null, a, true) : new DataPack[1];

        MPI.COMM_WORLD.Scatterv(packs1, 0, packIndexCount, packDisplacement, MPI.OBJECT, packs1, 0, packIndexCount[rank], MPI.OBJECT, 0);

        //распаковка данных с 0 процесса
        MO = packs1[0].getMatrix2();
        MB = packs1[0].getMatrix1();
        a = packs1[0].getConstant();

        //распаковка данных с p процесса
        DataPack pack = rank == p ? packs[p] : packs[0];
        MC = pack.getMatrix1();
        MK = pack.getMatrix2();
        Z = pack.getVector();

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


    //TODO: рефакторинг
    //TODO: попытаться сделать одновременную посылку - так быстрее будет

    private static void initData(int rank) {
        if (rank == 0) {
            MB = MatrixOperations.inputMatrix(N);
            MO = MatrixOperations.inputMatrix(N);
            a = MatrixOperations.inputConstant();

        } else if (rank == p) {
            MC = MatrixOperations.inputMatrix(N);
            MK = MatrixOperations.inputMatrix(N);
            Z = MatrixOperations.inputVector(N);
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