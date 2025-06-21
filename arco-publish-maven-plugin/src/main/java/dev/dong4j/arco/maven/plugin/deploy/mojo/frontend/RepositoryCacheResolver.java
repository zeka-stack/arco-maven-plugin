package dev.dong4j.arco.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.CacheDescriptor;
import com.github.eirslett.maven.plugins.frontend.lib.CacheResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;

import java.io.File;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.7.0
 */
public class RepositoryCacheResolver implements CacheResolver {

    /** GROUP_ID */
    private static final String GROUP_ID = "com.github.eirslett";
    /** Repository system session */
    private final RepositorySystemSession repositorySystemSession;

    /**
     * Repository cache resolver
     *
     * @param repositorySystemSession repository system session
     * @since 1.7.0
     */
    public RepositoryCacheResolver(RepositorySystemSession repositorySystemSession) {
        this.repositorySystemSession = repositorySystemSession;
    }

    /**
     * Resolve
     *
     * @param cacheDescriptor cache descriptor
     * @return the file
     * @since 1.7.0
     */
    @Override
    public File resolve(CacheDescriptor cacheDescriptor) {
        LocalRepositoryManager manager = this.repositorySystemSession.getLocalRepositoryManager();
        return new File(
            manager.getRepository().getBasedir(),
            manager.getPathForLocalArtifact(this.createArtifact(cacheDescriptor))
        );
    }

    /**
     * Create artifact
     *
     * @param cacheDescriptor cache descriptor
     * @return the default artifact
     * @since 1.7.0
     */
    private DefaultArtifact createArtifact(CacheDescriptor cacheDescriptor) {
        String version = cacheDescriptor.getVersion().replaceAll("^v", "");

        DefaultArtifact artifact;

        if (cacheDescriptor.getClassifier() == null) {
            artifact = new DefaultArtifact(
                GROUP_ID,
                cacheDescriptor.getName(),
                cacheDescriptor.getExtension(),
                version
            );
        } else {
            artifact = new DefaultArtifact(
                GROUP_ID,
                cacheDescriptor.getName(),
                cacheDescriptor.getClassifier(),
                cacheDescriptor.getExtension(),
                version
            );
        }
        return artifact;
    }
}
