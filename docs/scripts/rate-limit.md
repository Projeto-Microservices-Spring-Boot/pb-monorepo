# Rate Limit

## Testa Rate Limit em rotas publicas

```bash
for ($i=1; $i -le 5; $i++) {
  $c = curl -s -o $null -w "%{http_code}" http://localhost:8000/users/public/actuator/health
  if ($c -eq 200) { Write-Host "$i`: $c OK" -ForegroundColor Green }
  elseif ($c -eq 429) { Write-Host "$i`: $c RATE LIMITED" -ForegroundColor Red }
  else { Write-Host "$i`: $c" -ForegroundColor Yellow }
}
```

```txt
1: {"status":"UP"}
2: {"status":"UP"}
3: {"message":"API rate limit exceeded",   "request_id":"0ebf6d0f2a25a70056c7953981330019"}
4: {"message":"API rate limit exceeded",   "request_id":"04bf562e045d823e8d9c93168d46c621"}
5: {"message":"API rate limit exceeded",   "request_id":"03c7eb10a574736c89e98a1b889f4844"}
```

## Testa Rate Limit em rotas autenticadas

```bash
$token = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJmcm9udGVuZCIsIm5hbWUiOiJrb25nIGFwaSIsInN1YiI6ImZiMGExYTA0LWQ0ZWQtNDMzOS04MWEwLWQ0ZThlYjRkYTc0MyIsInJvbGUiOiJCVVlFUiIsImV4cCI6MTc4MTA0NDIzOSwiaWF0IjoxNzgxMDQzOTM5fQ.pDVRoSfjVvaqiB2cCSkStdvE_B09Tc-YuXmjlGZ5A1vxZcBOk-dnnKwj9hxMobHat2Evfd3KrIGt7U_Thph3r6v8RVR8ozwJGzCrqOm5lHiI1hw3UqHi5_BqAlB6hI67b-XLs6t6ZGnwY1BDJUTHKMxCZaLsHpCjwzAGkXWGbmAyOE9TLkZCBI-8R-YDLFzCalfU9APMTTnFfZT8AqM_r_MtQ1G3eRH1O77NWX-etXaar9J8aVtilGo693LutmPf_on-PT2Eb7vzBChqiXaaKg8TcOMWrVR5yoV_uSikcpfKQWQuEWQM0l21OBVOdGaDDaXloYRkPvewcu3u0_DOyQ"
for ($i=1; $i -le 10; $i++) {
  $c = curl -s -o $null -w "%{http_code}" -H "Authorization: Bearer $token" http://localhost:8000/users/me
  if ($c -eq 200) { Write-Host "$i`: $c OK" -ForegroundColor Green }
  elseif ($c -eq 429) { Write-Host "$i`: $c RATE LIMITED" -ForegroundColor Red }
  else { Write-Host "$i`: $c" -ForegroundColor Yellow }
  Start-Sleep -Milliseconds 100
}
```

```txt
1: {"name":"kong api"}
2: {"name":"kong api"}
3: {"name":"kong api"}
4: {"name":"kong api"}
5: {"name":"kong api"}
6: {"name":"kong api"}
7: {"name":"kong api"}
8: {"name":"kong api"}
9: {"message":"API rate limit exceeded",   "request_id":"90ee555256c47e144bc2f836be2e9535"}
10: {"message":"API rate limit exceeded",   "request_id":"cc000e7adac7b35b3c4fe46a90aedb1c"}
```
