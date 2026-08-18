# Sentinel 规则种子脚本：向 Nacos 发布初始流控/熔断规则（group=SENTINEL_GROUP，JSON 需 url-encode）
# 何时重跑：Nacos 容器重建后（单机模式为 Derby 内嵌存储，docker compose down/up 会丢配置）；
# 日常改规则去 Nacos 控制台直接编辑对应 dataId，发布后秒推到服务，无需重跑本脚本
$ns = 'http://localhost:8848'

# 0) remove probe config
curl.exe -s -X DELETE "$ns/nacos/v1/cs/configs?dataId=probe-test&group=SENTINEL_GROUP" | Out-Null

# 1) lab-product flow rules: /api/products QPS=2 (restore today's rule as the first persistent one)
$productFlow = '[{"resource":"/api/products","grade":1,"count":2,"strategy":0,"controlBehavior":0,"clusterMode":false}]'
# 2) lab-order flow rules: empty (placeholder, edit later in Nacos console)
$orderFlow = '[]'
# 3) degrade rules: empty placeholders
$productDegrade = '[]'
$orderDegrade = '[]'

$items = @(
    @{ id = 'lab-product-flow-rules';   content = $productFlow },
    @{ id = 'lab-order-flow-rules';     content = $orderFlow },
    @{ id = 'lab-product-degrade-rules'; content = $productDegrade },
    @{ id = 'lab-order-degrade-rules';   content = $orderDegrade }
)

foreach ($it in $items) {
    $r = curl.exe -s -X POST "$ns/nacos/v1/cs/configs" --data "dataId=$($it.id)" --data "group=SENTINEL_GROUP" --data "type=JSON" --data-urlencode "content=$($it.content)"
    Write-Output ("publish " + $it.id + " -> " + $r)
}

Start-Sleep -Seconds 2
Write-Output ''
Write-Output '===== read back ====='
foreach ($it in $items) {
    $g = curl.exe -s "$ns/nacos/v1/cs/configs?dataId=$($it.id)&group=SENTINEL_GROUP"
    Write-Output ($it.id + " : " + $g)
}
