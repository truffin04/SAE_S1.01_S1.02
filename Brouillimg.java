import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Brouillimg {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java Brouillimg <image_claire> <clé> [image_sortie]");
            System.exit(1);
        }

        String inPath = args[0];
        String outPath = (args.length >= 3) ? args[2] : "out.png";

        // Masque 0x7FFF pour garantir que la clé ne dépasse pas les 15 bits

        int key = Integer.parseInt(args[1]) & 0x7FFF;
        BufferedImage inputImage = ImageIO.read(new File(inPath));

        if (inputImage == null) {
            throw new IOException("Format d’image non reconnu: " + inPath);
        }

        final int height = inputImage.getHeight();
        final int width = inputImage.getWidth();
        System.out.println("Dimensions de l'image : " + width + "x" + height);

        // Pré‑calcul des lignes en niveaux de gris pour accélérer le calcul du critère

        int[][] inputImageGL = rgb2gl(inputImage);
        int[] perm = generatePermutation(height, key);
        BufferedImage scrambledImage = scrambleLines(inputImage, perm);
        ImageIO.write(scrambledImage, "png", new File(outPath));
        System.out.println("Image écrite: " + outPath);

    }

    /**
     * 
     * Convertit une image RGB en niveaux de gris (GL).
     * 
     * @param inputRGB image d'entrée en RGB
     * 
     * @return tableau 2D des niveaux de gris (0-255)
     * 
     */

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

                // luminance simple (évite float)

                int gray = (r * 299 + g * 587 + b * 114) / 1000;
                outGL[y][x] = gray;

            }

        }
        return outGL;
    }

    /**
     * 
     * Génère une permutation des entiers 0..size-1 en fonction d'une clé.
     * 
     * @param size taille de la permutation
     * 
     * @param key  clé de génération (15 bits)
     * 
     * @return tableau de taille 'size' contenant une permutation des entiers
     *         0..size-1
     * 
     */

    public static int[] generatePermutation(int size, int key) {
        int[] scrambleTable = new int[size];

        for (int i = 0; i < size; i++) {
            scrambleTable[i] = i;
            scrambleTable[i] = scrambledId(i, size, key);
        }
        return scrambleTable;

    }

    /**
     * 
     * Mélange les lignes d'une image selon une permutation donnée.
     * 
     * @param inputImg image d'entrée
     * 
     * @param perm     permutation des lignes (taille = hauteur de l'image)
     * 
     * @return image de sortie avec les lignes mélangées
     * 
     */

    public static BufferedImage scrambleLines(BufferedImage inputImg, int[] perm) {
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();

        // Vérifie que la taille de la permutation correspond à la hauteur de l'image
        if (perm.length != height) {
            throw new IllegalArgumentException("Taille d'image <> taille permutation");
        }

        // Crée une nouvelle image de sortie
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Pour chaque ligne de l'image de sortie
        for (int y = 0; y < height; y++) {
            int srcY = perm[y]; // Ligne source à copier
            // Pour chaque pixel de la ligne
            for (int x = 0; x < width; x++) {
                // Récupère le pixel de l'image d'entrée
                int pixel = inputImg.getRGB(x, srcY);
                // Écrit le pixel dans l'image de sortie
                out.setRGB(x, y, pixel);
            }
        }

        // Retourne l'image de sortie
        return out;
    }

    /**
     * 
     * Renvoie la position de la ligne id dans l'image brouillée.
     * 
     * @param id   indice de la ligne dans l'image claire (0..size-1)
     * 
     * @param size nombre total de lignes dans l'image
     * 
     * @param key  clé de brouillage (15 bits)
     *
     * @return indice de la ligne dans l'image brouillée (0..size-1)
     * 
     */

    public static int scrambledId(int id, int size, int key) {

        // valeur binaire de la clé en &(et logique = multiplication binaire) avec 0x7F
        // = 01111111
        // => conserve les 7 dernier bits   
        int s = key & 0x7F;

        // >> décalage de 7 bit vers la droite pour conserver les 8 premiers bits de
        // poids fort
        int r = key >> 7;

        return (r + (2 * s + 1) * id) % size;

    }

}