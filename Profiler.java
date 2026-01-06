import java.util.function.Function;
import java.util.function.Supplier;

public class Profiler {

    /**
     * Mesure le temps d'exécution d'une méthode donnée.
     * @param method La méthode à exécuter (sous forme de Supplier)
     * @return Le temps d'exécution en secondes
     */
    public static double analyse(Supplier<?> method) {
        long start = System.nanoTime();
        method.get(); // Exécute la méthode
        long end = System.nanoTime();
        return (end - start) / 1e9; // Retourne le temps en secondes
    }

    /**
     * Si clock0 est >0, retourne une chaîne de caractères
     * représentant la différence de temps depuis clock0.
     * @param clock0 instant initial
     * @return expression du temps écoulé depuis clock0
     */
    public static String timestamp(long clock0) {
        String result = null;

        if (clock0 > 0) {
            double elapsed = (System.nanoTime() - clock0) / 1e9;
            String unit = "s";
            if (elapsed < 1.0) {
                elapsed *= 1000.0;
                unit = "ms";
            }
            result = String.format("%.4g%s elapsed", elapsed, unit);
        }
        return result;
    }

    /**
     * Retourne l'heure courante en ns.
     * @return
     */
    public static long timestamp() {
        return System.nanoTime();
    }
}
