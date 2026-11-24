$(document).ready(function () {
    const params = new URLSearchParams(window.location.search);
    const fishkey = params.get("fish");

    /** 개행 문자(\n)로 구분된 텍스트를 <li> 태그 리스트로 변환 */
    function renderListItems(text) {
        if (!text) return "";
        if (text.includes("<li>")) return text; // 레거시 HTML 태그 호환
        return text.split(/\r?\n/)
            .map(line => line.trim())
            .filter(line => line.length > 0)
            .map(line => `<li>${line}</li>`)
            .join("");
    }

    // 기존: fetch("data/data_fish.json") → 단건 API /api/fish/{key} 로 변경
    fetch("/aquamate/api/fish/" + fishkey)
        .then(response => {
            if (!response.ok) {
                alert("해당 물고기 정보를 찾을 수 없습니다.");
                return null;
            }
            return response.json();
        })
        .then(fishData => {
            if (!fishData) return;

            if (fishData.img) $("#info_main img").attr("src", "/aquamate/" + fishData.img);
            if (fishData.name) $("#info_main").find("h1").text(fishData.name);
            if (fishData.basic) $("#info_basic").html(renderListItems(fishData.basic));
            if (fishData.env) $("#info_env").html(renderListItems(fishData.env));
            if (fishData.type) $("#info_type").text(fishData.type);
            if (fishData.taming) $("#info_taming").text(fishData.taming);
        })
        .catch(error => {
            console.error("물고기 정보 불러오기 실패 : ", error);
            alert("물고기 정보를 불러오는 데 실패했습니다.");
        })

})