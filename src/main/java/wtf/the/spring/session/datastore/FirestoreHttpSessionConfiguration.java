package wtf.the.spring.session.datastore;

import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

@Configuration
public class FirestoreHttpSessionConfiguration extends SpringHttpSessionConfiguration implements ImportAware {

    private String collection = null;
    private Duration ttl = null;

    @Override
    public void setImportMetadata(AnnotationMetadata meta) {
        var map = meta.getAnnotationAttributes(EnableFirestoreHttpSession.class.getName());
        var attrs = requireNonNull(AnnotationAttributes.fromMap(map));
        collection = attrs.getString("collection");
        ttl = Duration.of(
            attrs.getNumber("ttl"),
            attrs.getEnum("ttlUnit")
        );
    }

    @Bean
    public FirestoreSessionRepository sessionRepository(Firestore firestore) {
        return new FirestoreSessionRepository(firestore.collection(collection), ttl);
    }

    @Bean
    public FirestoreSessionCleaner sessionCleaner(Firestore firestore) {
        return new FirestoreSessionCleaner(firestore.collection(collection));
    }
}
