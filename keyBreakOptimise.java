//  SAE S1.01-02
//  Projet Java - Brouillage/Débrouillage d'image
//  Auteur - Nathan Tutin , Tom Ruffin
//  21/12/2025

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class keyBreakOptimise {

    // Convertit une image RGB en niveaux de gris
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

    // Calcule la distance euclidienne entre deux lignes
    public static double euclideanDistance(int[] x, int[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Les lignes doivent avoir la même taille");
        }
        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            double diff = x[i] - y[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    // Calcule le score euclidien total pour une image
    public static double scoreEuclidean(int[][] imageGL) {
        int height = imageGL.length;
        if (height < 2) {
            return 0.0;
        }
        double totalScore = 0.0;
        for (int i = 0; i < height - 1; i++) {
            double distance = euclideanDistance(imageGL[i], imageGL[i + 1]);
            totalScore += distance;
        }
        return totalScore;
    }

    // Calcule la corrélation de Pearson entre deux lignes
    public static double pearsonCorrelation(int[] line1, int[] line2) {
        if (line1.length != line2.length || line1.length == 0) {
            return 0;
        }
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
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

    // Calcule le score de Pearson moyen pour une image
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

    // Génère la position brouillée d'une ligne
    public static int scrambledId(int id, int size, int s, int r) {
        return (r + (2 * s + 1) * id) % size;
    }

    // Génère une permutation complète avec s et r
    public static int[] generatePermutation(int size, int s, int r) {
        int[] perm = new int[size];
        for (int i = 0; i < size; i++) {
            perm[i] = scrambledId(i, size, s, r);
        }
        return perm;
    }

    // Permute les lignes d'une matrice selon la permutation
    public static int[][] permuteLines(int[][] matrix, int[] perm) {
        int height = matrix.length;
        int width = matrix[0].length;
        int[][] permutedMatrix = new int[height][width];
        for (int y = 0; y < height; y++) {
            permutedMatrix[y] = matrix[perm[y]].clone();
        }
        return permutedMatrix;
    }

    // Débrouille l'image avec la permutation donnée
    public static BufferedImage unScrambleLines(BufferedImage inputImg, int[] perm) {
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();
        if (perm.length != height) {
            throw new IllegalArgumentException("Taille d'image <> taille permutation");
        }
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int srcY = perm[y];
            for (int x = 0; x < width; x++) {
                int pixel = inputImg.getRGB(x, srcY);
                out.setRGB(x, y, pixel);
            }
        }
        return out;
    }

    /**
     * MÉTHODE OPTIMISÉE EN 2 ÉTAPES
     * Étape 1 : Trouve S (7 bits = 128 valeurs) avec distance euclidienne
     * Étape 2 : Trouve R (8 bits = 256 valeurs) avec Pearson
     */
    public static int[] breakKeyOptimized(BufferedImage scrambledImage) {
        int height = scrambledImage.getHeight();
        int[][] scrambledMatrix = rgb2gl(scrambledImage);

        System.out.println("=== CASSAGE DE CLE OPTIMISE ===\n");

        // ÉTAPE 1 : Recherche de S avec Distance Euclidienne
        System.out.println("ETAPE 1/2 : Recherche de S (128 valeurs)");
        System.out.println("Methode : Distance Euclidienne\n");

        int bestS = 0;
        double bestScoreS = Double.POSITIVE_INFINITY;

        for (int s = 0; s < 128; s++) {
            int[] perm = generatePermutation(height, s, 0);
            int[][] unscrambledMatrix = permuteLines(scrambledMatrix, perm);
            double score = scoreEuclidean(unscrambledMatrix);

            if (score < bestScoreS) {
                bestScoreS = score;
                bestS = s;
                System.out.println("Nouveau meilleur S : " + bestS + " (score: " + bestScoreS + ")");
            }

            if ((s + 1) % 32 == 0) {
                System.out.println("Progression: " + (s + 1) + "/128");
            }
        }

        System.out.println("\nS trouve : " + bestS);
        System.out.println("Score euclidien : " + bestScoreS);

        // ÉTAPE 2 : Recherche de R avec Corrélation de Pearson
        System.out.println("\nETAPE 2/2 : Recherche de R (256 valeurs)");
        System.out.println("Methode : Correlation de Pearson\n");

        int bestR = 0;
        double bestScoreR = -Double.MAX_VALUE;

        for (int r = 0; r < 256; r++) {
            int[] perm = generatePermutation(height, bestS, r);
            int[][] unscrambledMatrix = permuteLines(scrambledMatrix, perm);
            double score = scorePearson(unscrambledMatrix);

            if (score > bestScoreR) {
                bestScoreR = score;
                bestR = r;
                System.out.println("Nouveau meilleur R : " + bestR + " (score: " + bestScoreR + ")");
            }

            if ((r + 1) % 64 == 0) {
                System.out.println("Progression: " + (r + 1) + "/256");
            }
        }

        System.out.println("\nR trouve : " + bestR);
        System.out.println("Score Pearson : " + bestScoreR);

        // Reconstruction de la clé complète
        int finalKey = (bestR << 7) | bestS;

        System.out.println("\n=== RESULTAT FINAL ===");
        System.out.println("S (step)    : " + bestS + " (7 bits)");
        System.out.println("R (offset)  : " + bestR + " (8 bits)");
        System.out.println("Cle finale  : " + finalKey + " (15 bits)");

        return new int[]{bestS, bestR, finalKey};
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java keyBreakOptimise <image_brouillee>");
            System.exit(1);
        }

        String imagePath = args[0];
        BufferedImage image = ImageIO.read(new File(imagePath));
        
        if (image == null) {
            System.err.println("Impossible de lire l'image: " + imagePath);
            System.exit(1);
        }

        System.out.println("Image: " + imagePath);
        System.out.println("Dimensions: " + image.getWidth() + "x" + image.getHeight());
        System.out.println("Tests a effectuer: 128 + 256 = 384 (au lieu de 32768)\n");

        long startTime = System.currentTimeMillis();
        int[] result = breakKeyOptimized(image);
        long endTime = System.currentTimeMillis();

        int bestS = result[0];
        int bestR = result[1];
        int finalKey = result[2];

        System.out.println("\nTemps d'execution: " + (endTime - startTime) + " ms");


        double timeTaken = Profiler.analyse(() -> breakKeyOptimized(image));
        System.out.println("\nTemps d'exécution: " + String.format("%.3f", timeTaken * 1000) + " ms");

        // Sauvegarde de l'image débrouillée
        int height = image.getHeight();
        int[] perm = generatePermutation(height, bestS, bestR);
        BufferedImage unscrambledImage = unScrambleLines(image, perm);
        
        String outputPath = "unscrambled_optimized_key" + finalKey + ".png";
        ImageIO.write(unscrambledImage, "png", new File(outputPath));
        System.out.println("Image debrouillee sauvegardee: " + outputPath);
    }
}