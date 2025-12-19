import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class cassagePearson {

    public static int[][] rgb2gl(BufferedImage inputRGB) {
        final int height = inputRGB.getHeight();
        final int width = inputRGB.getWidth();
        int[][] outGL = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = inputRGB.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int gray = (r * 299 + g * 587 + b * 114) / 1000;
                outGL[y][x] = gray;
            }
        }
        return outGL;
    }

    public static double pearsonCorrelation(int[] line1, int[] line2) {
        if (line1.length != line2.length || line1.length == 0) {
            return 0;
        }

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;
        double sumY2 = 0;
        int n = line1.length;

        for (int i = 0; i < n; i++) {
            sumX += line1[i];
            sumY += line2[i];
            sumXY += line1[i] * line2[i];
            sumX2 += line1[i] * line1[i];
            sumY2 += line2[i] * line2[i];
        }

        double moyenneX = sumX / n;
        double moyenneY = sumY / n;

        double numerateur = sumXY - n * moyenneX * moyenneY;
        double denominateurX = Math.sqrt(sumX2 - n * moyenneX * moyenneX);
        double denominateurY = Math.sqrt(sumY2 - n * moyenneY * moyenneY);

        if (denominateurX == 0 || denominateurY == 0) {
            return 0;
        }

        return numerateur / (denominateurX * denominateurY);
    }

    public static double scorePearson(int[][] image) {
        double score = 0;
        int count = 0;
        for (int i = 0; i < image.length - 1; i++) {
            double corr = pearsonCorrelation(image[i], image[i + 1]);
            score += corr;
            count++;
        }
        return count > 0 ? score / count : 0;
    }

    public static int[] generatePermutation(int size, int key) {
        int[] scrambleTable = new int[size];
        for (int i = 0; i < size; i++) {
            scrambleTable[i] = scrambledId(i, size, key);
        }
        return scrambleTable;
    }

    /**
     * Génère la permutation INVERSE pour le débrouillage
     * Si lors du brouillage: ligne i → position perm[i]
     * Alors pour débrouiller: la ligne à position perm[i] doit revenir en i
     * Donc: inversePerm[perm[i]] = i
     */
    public static int[] generateInversePermutation(int size, int key) {
        int[] perm = generatePermutation(size, key);
        int[] inversePerm = new int[size];
        
        for (int i = 0; i < size; i++) {
            inversePerm[perm[i]] = i;
        }
        
        return inversePerm;
    }

    public static int scrambledId(int id, int size, int key) {
        int s = key & 0x7F;
        int r = key >> 7;
        return (r + (2 * s + 1) * id) % size;
    }

    /**
     * Débrouille une image brouillée
     * L'image brouillée a la ligne originale i à la position perm[i]
     * Pour reconstruire: on lit à position perm[i] et on écrit à i
     */
    public static int[][] unScrambleMatrix(int[][] scrambledMatrix, int[] perm) {
        int height = scrambledMatrix.length;
        int width = scrambledMatrix[0].length;
        int[][] unscrambled = new int[height][width];
        
        for (int i = 0; i < height; i++) {
            // La ligne originale i se trouve à la position perm[i] dans l'image brouillée
            unscrambled[i] = scrambledMatrix[perm[i]].clone();
        }
        
        return unscrambled;
    }

    public static BufferedImage unScrambleLines(BufferedImage inputImg, int[] perm) {
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();
        if (perm.length != height) {
            throw new IllegalArgumentException("Taille d'image <> taille permutation");
        }
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            // Pour reconstruire la ligne y originale, on la prend à la position perm[y]
            int srcY = perm[y];
            for (int x = 0; x < width; x++) {
                int pixel = inputImg.getRGB(x, srcY);
                out.setRGB(x, y, pixel);
            }
        }
        return out;
    }

    /**
     * Permute les lignes d'une matrice de gris selon une permutation donnée.
     */
    public static int[][] permuteLines(int[][] matrix, int[] perm) {
        int height = matrix.length;
        int width = matrix[0].length;
        int[][] permutedMatrix = new int[height][width];
        for (int y = 0; y < height; y++) {
            permutedMatrix[y] = matrix[perm[y]].clone();
        }
        return permutedMatrix;
    }

    /**
     * Tente de casser la clé en essayant toutes les clés possibles.
     */
    public static int breakKey(BufferedImage scrambledImage) {
        int maxKey = 0;
        double maxScore = -Double.MAX_VALUE;
        int height = scrambledImage.getHeight();

        // 1. Convertir une seule fois l'image brouillée en niveaux de gris
        int[][] scrambledMatrix = rgb2gl(scrambledImage);

        // 2. Essayer toutes les clés possibles
        for (int key = 0; key < 32768; key++) {
            int[] perm = generatePermutation(height, key);
            // Permuter les lignes de la matrice de gris (au lieu de l'image)
            int[][] unscrambledMatrix = permuteLines(scrambledMatrix, perm);
            double score = scorePearson(unscrambledMatrix);

            if (score > maxScore) {
                maxScore = score;
                maxKey = key;
            }
        }

        return maxKey;
    }

    public static void main(String[] args) throws Exception {
        // Charger l'image brouillée
        BufferedImage image = ImageIO.read(new File("out.jpg"));
        System.out.println("Dimensions de l'image: " + image.getWidth() + "x" + image.getHeight());

        // Casser la clé
        long startTime = System.currentTimeMillis();
        int bestKey = breakKey(image);
        long endTime = System.currentTimeMillis();

        System.out.println("Meilleure clé trouvée : " + bestKey);
        System.out.println("Temps d'exécution : " + (endTime - startTime) + " ms");

        // Débrouiller l'image avec la meilleure clé
        int height = image.getHeight();
        int[] perm = generatePermutation(height, bestKey);
        BufferedImage unscrambledImage = unScrambleLines(image, perm);
        ImageIO.write(unscrambledImage, "jpg", new File("unscrambled.jpg"));
        System.out.println("Image débrouillée sauvegardée : unscrambled.jpg");
    }
}