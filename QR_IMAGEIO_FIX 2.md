# Fix: QR Code Download - ImageIO Headless Mode

## Problema
Errore durante il download del QR code:
```
java.lang.NoClassDefFoundError: Could not initialize class javax.imageio.ImageIO
```

Causa: Su server senza GUI (headless environment), `javax.imageio.ImageIO` non riesce ad inizializzarsi perché tenta di caricare risorse AWT non disponibili.

## Soluzione Implementata
Aggiunto il flag JVM `-Djava.awt.headless=true` nel `pom.xml`:
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>-Djava.awt.headless=true</jvmArguments>
        ...
    </configuration>
</plugin>
```

## Come Usare

### 1. Utilizzando Maven (raccomandato per dev)
```bash
cd ordering-system
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Il flag headless è automaticamente applicato.

### 2. Esecuzione diretta del JAR
Se esegui il JAR compilato direttamente, DEVI aggiungere il flag:
```bash
java -Djava.awt.headless=true -jar ordering-system-0.0.1-SNAPSHOT.jar
```

### 3. Con Docker
Aggiungi il flag all'`ENTRYPOINT`:
```dockerfile
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
```

### 4. Con Docker Compose
```yaml
services:
  backend:
    environment:
      - JAVA_OPTS=-Djava.awt.headless=true
```

## Come Testare

1. Avvia il backend:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. Dalla dashboard staff, vai a "Postazioni"

3. Clicca il menu di una postazione (⋮) 

4. Prova a scaricare il QR - dovrebbe funzionare senza errori

5. Verifica nel browser che il PNG sia valido

## Spiegazione Tecnica

- **java.awt.headless=true**: Comunica a Java di operare senza display server
- **ImageIO**: Usa il supporto PNG built-in di Java (non richiede X11/Wayland)
- **ZXing**: Genera la matrice QR; `MatrixToImageWriter` la converte in BufferedImage
- **PNG encoding**: Usa i codec built-in di Java, compatibili con headless mode
