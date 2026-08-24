# Kubernetes manifests

The raw reference deployment includes a restricted namespace, non-token-bearing runner account,
environment ConfigMap, empty Secret template, Allure PVC, Selenium Hub, autoscaled Chrome/Firefox
nodes, one-off Job, and nightly CronJob. Workloads define probes and bounded resources.

```powershell
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/platform-config.yaml
kubectl apply -f kubernetes/allure-storage.yaml
kubectl apply -f kubernetes/selenium-grid.yaml
kubectl apply -f kubernetes/test-execution.yaml
kubectl -n urbanflow-quality wait --for=condition=available deployment/selenium-hub --timeout=180s
```

Replace the example test-runner image and populate `test-secrets-template` through a secret manager
before creating Jobs. For kind or minikube, install metrics-server before validating the HPAs.
