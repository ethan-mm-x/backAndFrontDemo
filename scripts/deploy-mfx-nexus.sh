#!/usr/bin/env bash
# 将 mfx-spring-boot-starter 发布到公司 Nexus Snapshots。
# 说明：部分环境 mvn deploy 对 Nexus 2.x 会出现 NoHttpResponseException，本脚本用 HTTP PUT 上传。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODULE="$ROOT/mfx-spring-boot-starter"
SETTINGS="$MODULE/.mvn/settings-deploy.xml"
REPO_BASE="${MFX_NEXUS_SNAPSHOTS_URL:-http://10.126.138.142:8081/nexus/content/repositories/snapshots}"
GROUP_PATH="com/mfx/mfx-spring-boot-starter"
VERSION="0.0.1-SNAPSHOT"
ARTIFACT="mfx-spring-boot-starter"

if [[ ! -f "$SETTINGS" ]]; then
  echo "缺少 $SETTINGS"
  echo "请先: cp $MODULE/.mvn/settings-deploy.xml.example $SETTINGS 并填入密码"
  exit 1
fi

USER=$(sed -n 's|.*<username>\(.*\)</username>.*|\1|p' "$SETTINGS" | head -1)
PASS=$(sed -n 's|.*<password>\(.*\)</password>.*|\1|p' "$SETTINGS" | head -1)
if [[ -z "$USER" || -z "$PASS" || "$PASS" == "CHANGE_ME" ]]; then
  echo "请在 $SETTINGS 中配置有效的 username/password"
  exit 1
fi

echo "==> package"
cd "$MODULE"
mvn -s .mvn/settings-deploy.xml -q clean package -DskipTests

JAR="$MODULE/target/${ARTIFACT}-${VERSION}.jar"
POM="$MODULE/pom.xml"
BASE="${REPO_BASE}/${GROUP_PATH}"
AUTH="${USER}:${PASS}"
TS=$(date -u +%Y%m%d%H%M%S)

upload() {
  local remote="$1"
  local file="$2"
  local ctype="$3"
  local code
  code=$(curl -s -o /tmp/mfx-deploy.out -w "%{http_code}" --connect-timeout 15 --max-time 120 \
    -u "$AUTH" -X PUT --data-binary @"$file" -H "Content-Type: $ctype" "$remote")
  echo "PUT $remote -> HTTP $code"
  if [[ "$code" != "200" && "$code" != "201" ]]; then
    cat /tmp/mfx-deploy.out
    exit 1
  fi
}

echo "==> upload artifacts"
upload "$BASE/${VERSION}/${ARTIFACT}-${VERSION}.pom" "$POM" "application/xml"
upload "$BASE/${VERSION}/${ARTIFACT}-${VERSION}.jar" "$JAR" "application/java-archive"

cat > /tmp/mfx-ver-meta.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<metadata modelVersion="1.1.0">
  <groupId>com.mfx</groupId>
  <artifactId>${ARTIFACT}</artifactId>
  <version>${VERSION}</version>
  <versioning>
    <snapshot>
      <timestamp>$(date -u +%Y%m%d.%H%M%S)</timestamp>
      <buildNumber>1</buildNumber>
    </snapshot>
    <lastUpdated>${TS}</lastUpdated>
    <snapshotVersions>
      <snapshotVersion>
        <extension>jar</extension>
        <value>${VERSION}</value>
        <updated>${TS}</updated>
      </snapshotVersion>
      <snapshotVersion>
        <extension>pom</extension>
        <value>${VERSION}</value>
        <updated>${TS}</updated>
      </snapshotVersion>
    </snapshotVersions>
  </versioning>
</metadata>
EOF

cat > /tmp/mfx-art-meta.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>com.mfx</groupId>
  <artifactId>${ARTIFACT}</artifactId>
  <versioning>
    <latest>${VERSION}</latest>
    <versions>
      <version>${VERSION}</version>
    </versions>
    <lastUpdated>${TS}</lastUpdated>
  </versioning>
</metadata>
EOF

upload "$BASE/${VERSION}/maven-metadata.xml" /tmp/mfx-ver-meta.xml "application/xml"
upload "$BASE/maven-metadata.xml" /tmp/mfx-art-meta.xml "application/xml"

echo "==> done"
echo "Nexus path: ${REPO_BASE}/${GROUP_PATH}/${VERSION}/"
echo "Coordinate: com.mfx:${ARTIFACT}:${VERSION}"
