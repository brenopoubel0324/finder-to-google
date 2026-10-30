/*
 * Bootstrap de transporte, executado uma vez com o source-file launcher do JDK 17+.
 * O build usa o Gradle Wrapper oficial, não uma implementação alternativa de Gradle.
 * Fonte do JAR: gradle/gradle, tag v8.13.0.
 * SHA-256 conferido em gradle/actions, sources/src/wrapper-validation/wrapper-checksums.json.
 */
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

class Bootstrap {
    static final String URL = "https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar";
    static final String SHA256 = "81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f";

    public static void main(String[] args) {
        Path temp = null;
        try {
            if (args.length != 1) throw new IllegalArgumentException();
            var builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL);
            // Respeitar o proxy herdado do ambiente; nunca desabilitar verificação TLS.
            String proxy = System.getenv("HTTPS_PROXY");
            if (proxy == null || proxy.isBlank()) proxy = System.getenv("https_proxy");
            if (proxy != null && !proxy.isBlank()) {
                URI uri = URI.create(proxy);
                builder.proxy(ProxySelector.of(new InetSocketAddress(uri.getHost(), uri.getPort() == -1 ? 80 : uri.getPort())));
            }
            System.err.println("Obtendo Gradle Wrapper oficial 8.13 (43 KB), com verificação SHA-256…");
            var request = HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(30)).GET().build();
            var response = builder.build().send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());
            byte[] bytes = response.body();
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!SHA256.equals(actual)) throw new SecurityException("Checksum divergente");
            Path destination = Path.of(args[0]).toAbsolutePath();
            Files.createDirectories(destination.getParent());
            temp = Files.createTempFile(destination.getParent(), "wrapper-", ".tmp");
            Files.write(temp, bytes);
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            System.err.println("Não foi possível preparar o Gradle Wrapper: " + exception.getClass().getSimpleName());
            System.err.println("É necessário acesso HTTPS a raw.githubusercontent.com. Consulte o README para instalação offline.");
            if (temp != null) try { Files.deleteIfExists(temp); } catch (Exception ignored) { }
            System.exit(1);
        }
    }
}
