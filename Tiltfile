# OS Identification
gradlew = "./gradlew"
expected_ref = "$EXPECTED_REF"
if os.name == "nt":
  gradlew = "gradlew.bat"
  expected_ref = "%EXPECTED_REF%"

# For buildPacks
strCommand = gradlew + ' bootBuildImage --imageName ' + expected_ref

# For Jib
strCommand = gradlew + ' jibDockerBuild --image ' + expected_ref

# Build
custom_build(
    # Name of the container image
    ref = 'order-service',
    # Command to build the container image
    command = strCommand,
    # Files to watch that trigger a new build
    deps = ['build.gradle', 'src']
)

# Deploy
k8s_yaml(kustomize('k8s')

# Manage
k8s_resource('order-service', port_forwards=['9002'])