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

        double sumX = 0, sumY = 0;
        double sumXY = 0, sumX2 = 0, sumY2 = 0;
        int n = line1.length;

        for (int i = 0; i < n; i++) {
            sumX += line1[i];
            sumY += line2[i];
            sumXY += line1[i] * line2[i];
            sumX2 += line1[i] * line1[i];
            sumY2 += line2[i] * line2[i];
        }

        double meanX = sumX / n;
        double meanY = sumY / n;

        double numerator = sumXY - n * meanX * meanY;
        double denominatorX = Math.sqrt(sumX2 - n * meanX * meanX);
        double denominatorY = Math.sqrt(sumY2 - n * meanY * meanY);

        if (denominatorX == 0 || denominatorY == 0) {
            return 0;
        }

        return numerator / (denominatorX * denominatorY);
    }

    public static double scorePearson(int[][] image) {
        double score = 0;
        for (int i = 0; i < image.length - 1; i++) {
            score += pearsonCorrelation(image[i], image[i + 1]);
        }
        return score;
    }

    public static void main(String[] args) throws Exception {
        // Charger l'image
        BufferedImage image = ImageIO.read(new File("image_retour.jpg"));
        
        // Convertir en niveaux de gris
        int[][] imageMatrix = rgb2gl(image);
        
        System.out.println("Dimensions de l'image: " + image.getWidth() + "x" + image.getHeight());
        System.out.println();
        
        // Afficher la corrélation de Pearson entre chaque paire de lignes consécutives
        System.out.println("Corrélations de Pearson entre lignes consécutives:");
        for (int i = 0; i < imageMatrix.length - 1; i++) {
            double correlation = pearsonCorrelation(imageMatrix[i], imageMatrix[i + 1]);
            System.out.println("Ligne " + i + " et " + (i + 1) + ": " + correlation);
        }
        
        System.out.println();
        
        // Calculer le score global de Pearson
        double score = scorePearson(imageMatrix);
        System.out.println("Score de Pearson global: " + score);
    }

}
