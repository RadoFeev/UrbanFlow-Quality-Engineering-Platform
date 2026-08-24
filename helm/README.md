# Helm chart

`urbanflow-test-infrastructure` parameterizes pinned test/Grid images, resources, Allure storage,
suite selection, browser capacity and autoscaling, retention, schedule, and an external Secret
reference. Defaults never contain secret values.

```powershell
helm lint helm/urbanflow-test-infrastructure
helm template urbanflow helm/urbanflow-test-infrastructure --values helm/urbanflow-test-infrastructure/values-minimal.yaml
helm upgrade --install urbanflow helm/urbanflow-test-infrastructure --namespace urbanflow-quality --create-namespace
```

For kind, create a cluster with a default StorageClass and metrics-server, load the test-runner image,
override `images.testRunner`, install the chart, and wait for Hub/node readiness. Minikube uses the
same flow after `minikube addons enable metrics-server`.
