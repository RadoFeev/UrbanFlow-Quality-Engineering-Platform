{{- define "urbanflow.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "urbanflow.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "urbanflow.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "urbanflow.namespace" -}}
{{- default .Release.Namespace .Values.namespace.name -}}
{{- end -}}

{{- define "urbanflow.labels" -}}
app.kubernetes.io/name: {{ include "urbanflow.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | quote }}
{{- end -}}

{{- define "urbanflow.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (printf "%s-runner" (include "urbanflow.fullname" .)) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{- define "urbanflow.secretName" -}}
{{- default (printf "%s-test-secrets" (include "urbanflow.fullname" .)) .Values.secrets.existingSecret -}}
{{- end -}}
