#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags for Arup-gope repository
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'gope.aust@gmail.com'
    // Use the branch defined in Jenkinsfile or default to master
    def gitBranch = env.GIT_BRANCH ?: 'master' 
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            # Configure Git identity
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            
            # Update main application deployment using YOUR Docker Hub namespace
            sed -i "s|image: arupgope/easyshop-app:.*|image: arupgope/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
            
            # Update migration job using YOUR Docker Hub namespace
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: arupgope/easyshop-migration:.*|image: arupgope/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            
            # Ensure ingress is using the correct domain
            // DELETE OR COMMENT OUT THIS SECTION IN YOUR SHARED LIBRARY:
/*
if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
    sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
fi
*/
            
            # Check for changes
            if git diff --quiet; then
                echo "No changes to commit"
            else
                # Commit changes
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                
                # REPO FIX: Set origin to YOUR application repo, not the shared library repo
                git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/Arup-gope/e-commerce-app.git

                # Push to the correct branch
                git push origin HEAD:${gitBranch}
            fi
        """
    }
}
