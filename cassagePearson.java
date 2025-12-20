//  SAE S1.01-02
//  Projet Java - Brouillage/Débrouillage d'image
//  Auteur - Nathan Tutin , Tom Ruffin
//  21/12/2025

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

    /** 
     * 
     * 
     * ===== Corrélation de Pearson =====
     * 
     * 
    */

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
     * Tente de casser la clé avec Pearson en essayant toutes les clés possibles.
     */
    public static int breakKeyPearson(BufferedImage scrambledImage) {
        int maxKey = 0;
        double maxScore = -Double.MAX_VALUE;
        int height = scrambledImage.getHeight();

        System.out.println("Méthode: Corrélation de Pearson");
        System.out.println("Test de 32768 clés...\n");

        // 1. Convertir une seule fois l'image brouillée en niveaux de gris
        int[][] scrambledMatrix = rgb2gl(scrambledImage);

        // 2. Essayer toutes les clés possibles
        for (int key = 0; key < 32768; key++) {
            int[] perm = generatePermutation(height, key);
            // Permuter les lignes de la matrice de gris
            int[][] unscrambledMatrix = permuteLines(scrambledMatrix, perm);
            double score = scorePearson(unscrambledMatrix);

            if (score > maxScore) {
                maxScore = score;
                maxKey = key;
                System.out.println("Nouvelle meilleure clé: " + maxKey + " (score: " + String.format("%.6f", maxScore) + ")");
            }

            // Affichage de progression
            if (key % 4000 == 0 && key > 0) {
                System.out.println("Progression: " + key + "/32768");
            }
        }

        System.out.println("\nScore final: " + String.format("%.6f", maxScore));
        return maxKey;
    }

    /** 
     * 
     * 
     * ===== Distance Euclidienne =====
     * 
     * 
    */

    public static double euclideanDistance(int[] x, int[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("les lignes doivent avoir la même taille");
        }

        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            double diff = x[i] - y[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

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

    /**
     * Tente de casser la clé avec la distance euclidienne en essayant toutes les clés possibles.
     */
    public static int breakKeyEuclidean(BufferedImage scrambledImage) {
        int bestKey = 0;
        double bestScore = Double.POSITIVE_INFINITY;
        int height = scrambledImage.getHeight();

        System.out.println("Méthode: Distance Euclidienne");
        System.out.println("Test de 32768 clés...\n");

        // Convertir l'image en niveaux de gris
        int[][] encryptedImageGL = rgb2gl(scrambledImage);

        for (int key = 0; key < 32768; key++) {
            int[] perm = generatePermutation(height, key);
            int[][] decryptedImageGL = permuteLines(encryptedImageGL, perm);
            double currentScore = scoreEuclidean(decryptedImageGL);
            
            // Pour la distance euclidienne, un score plus PETIT est meilleur
            if (currentScore < bestScore) {
                bestScore = currentScore;
                bestKey = key;
                System.out.println("Nouvelle meilleure clé: " + bestKey + " (score: " + String.format("%.2f", currentScore) + ")");
            }

            // Affichage de progression
            if (key % 4000 == 0 && key > 0) {
                System.out.println("Progression: " + key + "/32768");
            }
        }

        System.out.println("\nScore final: " + String.format("%.2f", bestScore));
        return bestKey;
    }

    public static void main(String[] args) throws Exception {
        // Vérification des arguments
        if (args.length < 2) {
            System.err.println("Usage: java cassagePearson <image_brouillée> <méthode>");
            System.err.println("  <méthode> peut être:");
            System.err.println("    - pearson    : Corrélation de Pearson");
            System.err.println("    - euclidean  : Distance Euclidienne");
            System.exit(1);
        }

        String imagePath = args[0];
        String method = args[1].toLowerCase();

        // Charger l'image brouillée
        BufferedImage image = ImageIO.read(new File(imagePath));
        
        if (image == null) {
            System.err.println("Impossible de lire l'image: " + imagePath);
            System.exit(1);
        }

        System.out.println("=== Cassage de clé ===");
        System.out.println("Image: " + imagePath);
        System.out.println("Dimensions: " + image.getWidth() + "x" + image.getHeight() + "\n");

        // Casser la clé selon la méthode choisie
        long startTime = System.currentTimeMillis();
        int bestKey;

        if (method.equals("pearson")) {
            bestKey = breakKeyPearson(image);
        } 
        else if (method.equals("euclidean") || method.equals("euclidienne")) {
            bestKey = breakKeyEuclidean(image);
        } 
        else {
            System.err.println("Méthode inconnue: " + method);
            System.err.println("Utilisez 'pearson' ou 'euclidean'");
            System.exit(1);
            return;
        }

        long endTime = System.currentTimeMillis();

        System.out.println("\n=== RÉSULTAT FINAL ===");
        System.out.println("Meilleure clé trouvée: " + bestKey);
        System.out.println("Temps d'exécution: " + (endTime - startTime) + " ms");

        // Débrouiller l'image avec la meilleure clé
        int height = image.getHeight();
        int[] perm = generatePermutation(height, bestKey);
        BufferedImage unscrambledImage = unScrambleLines(image, perm);
        
        String outputPath = "unscrambled_" + method + "_key" + bestKey + ".png";
        ImageIO.write(unscrambledImage, "png", new File(outputPath));
        System.out.println("Image débrouillée sauvegardée: " + outputPath);
    }
}