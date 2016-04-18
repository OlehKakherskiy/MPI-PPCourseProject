import mpi.*;

import java.util.Arrays;

public class Main {


    private static int[][] MA, MB, MO, MC, MK;
    private static int[] Z;
    private static int a, minZ, N, H;
    private static int p;

    public static void main(String[] args) {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        N = Integer.parseInt(args[args.length - 1]);
        H = N / size;
        p = size - 1;
        int startIndex = H * rank;
        int endIndex = rank == 0 ? N - H * p : H * (rank + 1);
//        System.out.println("index bounds in rank " + rank + ": " + startIndex + ", " + endIndex);
        int power = (int) (Math.log(size) / Math.log(2)); //степень каждой вершины
        MPI.COMM_WORLD.Create_graph(GraphTopology.calculateIndexes(power, size), GraphTopology.calculateEdges(power, size), false);
//        System.out.println("vertex " + rank + " neighbours: " + Arrays.toString(topology.Neighbours(rank)));

        int[] displacement = getEachOffset(size);
        int[] indexCount = getEachCount(size);

        //TODO: refactoring
        if (rank == 0) {
            MB = MatrixOperations.inputMatrix(N);
            MO = MatrixOperations.inputMatrix(N);
            a = MatrixOperations.inputConstant();
            MC = new int[indexCount[rank]][N];
            MK = new int[indexCount[rank]][N];
            Z = new int[indexCount[rank]];

        } else if (rank == size - 1) {
            MC = MatrixOperations.inputMatrix(N);
            MK = MatrixOperations.inputMatrix(N);
            Z = MatrixOperations.inputVector(N);
            MB = new int[N][N];
            MO = new int[indexCount[rank]][N];
        } else {
            MB = new int[N][N];
            MO = new int[indexCount[rank]][N];
            MC = new int[indexCount[rank]][N];
            MK = new int[indexCount[rank]][N];
            Z = new int[indexCount[rank]];
        }

        //получение данных с последнего процесса
        MPI.COMM_WORLD.Scatterv(Z, 0, indexCount, displacement, MPI.INT, Z, 0, indexCount[rank], MPI.INT, size - 1);
        MPI.COMM_WORLD.Scatterv(MC, 0, indexCount, displacement, MPI.OBJECT, MC, 0, indexCount[rank], MPI.OBJECT, size - 1);
        MPI.COMM_WORLD.Scatterv(MK, 0, indexCount, displacement, MPI.OBJECT, MK, 0, indexCount[rank], MPI.OBJECT, size - 1);
        int ar[] = {a};
        MPI.COMM_WORLD.Bcast(ar, 0, 1, MPI.INT, 0);
        a = ar[0];
        MPI.COMM_WORLD.Bcast(MB, 0, N, MPI.OBJECT, 0);
        if (rank == size - 1) {
            System.out.println(indexCount[size - 1]);
            System.out.println(displacement[size - 1]);
        }
        MPI.COMM_WORLD.Scatterv(MO, 0, indexCount, displacement, MPI.OBJECT, MO, 0, indexCount[rank], MPI.OBJECT, 0);
        MPI.COMM_WORLD.Barrier();
        //обрезаем Z в последнем процессе до размера H - оставляем только кусок, который положен.
        if (rank == size - 1) {
            Z = Arrays.copyOfRange(Z, Z.length - H, Z.length);
            int[][] newMC = new int[H][];
            int[][] newMK = new int[H][];
            for (int i = 0; i < newMC.length; i++) {
                newMC[i] = MC[N - H + i]; //TODO: systemarraycopy
                newMK[i] = MK[N - H + i];
            }
            MC = newMC;
            MK = newMK;
        } else if (rank == 0) {
            int count = indexCount[0];
            int[][] newMO = new int[count][];
            System.arraycopy(MO, 0, newMO, 0, count);
            MO = newMO;
        }

        //вычисление локальных минимумов
        int[] localMinimums = new int[1];
        localMinimums[0] = MatrixOperations.min(Z, 0, Z.length);
        MPI.COMM_WORLD.Reduce(localMinimums, 0, localMinimums, 0, 1, MPI.INT, MPI.MIN, size - 1);
        MPI.COMM_WORLD.Bcast(localMinimums, 0, 1, MPI.INT, size - 1);
        minZ = localMinimums[0];
        System.out.println("minZ in " + rank + " =" + minZ);
        System.out.println("MC = " + Arrays.deepToString(MC));
        System.out.println("M0 = " + Arrays.deepToString(MO));
        int[][] buf = MatrixOperations.addMatrix(MC, MO, 1, 1);
        int[][] multRes = MatrixOperations.multMatrix(buf, MB);
        System.out.println("multRes = " + Arrays.deepToString(multRes));
        System.out.println("MK = " + Arrays.deepToString(MK));
        MA = MatrixOperations.addMatrix(multRes, MK, a, minZ);
        assert MA != null;
        System.out.println("MA = " + MA);

        int[][] result = new int[N][];
        MPI.COMM_WORLD.Gatherv(MA, 0, indexCount[rank], MPI.OBJECT, result, 0, indexCount, displacement, MPI.OBJECT, 0);

        if (rank == 0 && result.length <= 16)
            System.out.println(MatrixOperations.formattedDeepToString(result));
        System.out.println("Process " + rank + " is finished");
        MPI.Finalize();
    }

    //TODO: 1. чекнуть результаты
    //TODO: 2. комменты
    //TODO: 3. упаковка/распаковка
    //TODO: 4. рефакторинг
    //TODO: 2. тайминг


    private static int[] getEachCount(int processCount) {
        int[] result = new int[processCount];
        Arrays.fill(result, 1, result.length, H);
        //первый процесс получит больше элементов, если их неравное количество на каждый процесс
        result[0] = N - (result.length - 1) * H;
        return result;
    }

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