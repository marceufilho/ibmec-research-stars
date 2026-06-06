$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api/v1'

Write-Output '=== 1) LOGIN admin ==='
$login = Invoke-RestMethod -Uri "$base/auth/login" -Method Post -ContentType 'application/json' -Body (@{ email = 'admin@ibmec.br'; password = 'admin123' } | ConvertTo-Json)
$login | ConvertTo-Json
$token = $login.token

Write-Output '=== 2) GET /courses (autenticado) ==='
$courses = Invoke-RestMethod -Uri "$base/courses?size=3" -Headers @{ Authorization = "Bearer $token" }
$courses.content | ConvertTo-Json
Write-Output ("total cursos: " + $courses.totalElements)

Write-Output '=== 3) GET /courses SEM token (espera 401/403) ==='
try {
    Invoke-RestMethod -Uri "$base/courses" | Out-Null
    Write-Output 'ERRO: deveria ter bloqueado'
}
catch {
    Write-Output ('OK bloqueado: ' + [int]$_.Exception.Response.StatusCode + ' ' + $_.Exception.Response.StatusCode)
}

Write-Output '=== 4) REGISTER professor ==='
$courseIds = @($courses.content[0].id, $courses.content[1].id)
try {
    $reg = Invoke-RestMethod -Uri "$base/auth/register" -Method Post -ContentType 'application/json' -Body (@{
            name         = 'Prof Teste'
            email        = 'prof.teste@ibmec.br'
            lattesNumber = '1234567890123456'
            password     = 'senha123'
            courseIds    = $courseIds
        } | ConvertTo-Json)
    $reg | ConvertTo-Json
}
catch {
    Write-Output ('professor ja existe (ok para reexecucao): ' + [int]$_.Exception.Response.StatusCode)
}

Write-Output '=== 5) LOGIN professor + GET /professors/me ==='
$plogin = Invoke-RestMethod -Uri "$base/auth/login" -Method Post -ContentType 'application/json' -Body (@{ email = 'prof.teste@ibmec.br'; password = 'senha123' } | ConvertTo-Json)
$ptoken = $plogin.token
$me = Invoke-RestMethod -Uri "$base/professors/me" -Headers @{ Authorization = "Bearer $ptoken" }
$me | ConvertTo-Json

Write-Output '=== 6) Professor tenta GET /professors (espera 403) ==='
try {
    Invoke-RestMethod -Uri "$base/professors" -Headers @{ Authorization = "Bearer $ptoken" } | Out-Null
    Write-Output 'ERRO: professor nao deveria listar'
}
catch {
    Write-Output ('OK bloqueado: ' + [int]$_.Exception.Response.StatusCode + ' ' + $_.Exception.Response.StatusCode)
}

Write-Output '=== TODOS OS TESTES PASSARAM ==='
