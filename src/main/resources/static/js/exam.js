/* Java Silver 模擬試験 - クライアントサイド処理 */

(function () {
  "use strict";

  /* ---------------------------------------------------------------
     カウントダウンタイマー
     window.EXAM_END_TIME (epoch millis) が埋め込まれている前提
  --------------------------------------------------------------- */
  function formatRemaining(ms) {
    var totalSec = Math.max(Math.floor(ms / 1000), 0);
    var m = Math.floor(totalSec / 60);
    var s = totalSec % 60;
    return String(m).padStart(2, "0") + ":" + String(s).padStart(2, "0");
  }

  function startCountdown() {
    var el = document.getElementById("timerValue");
    if (!el) return;

    // 試験が終了している場合は、終了時点の残り時間で固定表示するだけでカウントは進めない
    if (window.EXAM_FINISHED) {
      el.textContent = formatRemaining(window.EXAM_REMAINING_MILLIS || 0);
      return;
    }

    if (typeof window.EXAM_END_TIME === "undefined") return;

    function tick() {
      var remain = window.EXAM_END_TIME - Date.now();
      if (remain <= 0) {
        el.textContent = "00:00";
        el.classList.add("warning");
        clearInterval(timerId);
        // サーバー側に時間切れを通知して結果ページへ
        window.location.href = "/exam/finish";
        return;
      }
      el.textContent = formatRemaining(remain);
      if (remain <= 5 * 60 * 1000) {
        el.classList.add("warning");
      }
    }
    tick();
    var timerId = setInterval(tick, 1000);
  }

  /* ---------------------------------------------------------------
     単一選択問題での最大選択数チェック
     (data-max 属性 = 選べる最大数)
  --------------------------------------------------------------- */
  function setupMaxSelectGuard() {
    var group = document.querySelector(".options-list");
    if (!group) return;
    var max = parseInt(group.getAttribute("data-max"), 10);
    if (!max || max < 1) return;

    var checkboxes = group.querySelectorAll('input[type="checkbox"]');
    checkboxes.forEach(function (cb) {
      cb.addEventListener("change", function () {
        var checkedCount = group.querySelectorAll('input[type="checkbox"]:checked').length;
        if (checkedCount > max) {
          cb.checked = false;
          showMaxSelectModal();
        }
      });
    });
  }

  /* ---------------------------------------------------------------
     モーダル制御（共通）
  --------------------------------------------------------------- */
  function openModal(id) {
    var m = document.getElementById(id);
    if (m) m.classList.add("open");
  }
  function closeModal(id) {
    var m = document.getElementById(id);
    if (m) m.classList.remove("open");
  }
  window.openModal = openModal;
  window.closeModal = closeModal;

  function showMaxSelectModal() {
    openModal("maxSelectModal");
  }

  /* ---------------------------------------------------------------
     「試験を終了する」確認モーダル
  --------------------------------------------------------------- */
  function setupFinishConfirm() {
    var finishBtn = document.getElementById("finishBtn");
    if (!finishBtn) return;
    finishBtn.addEventListener("click", function (e) {
      e.preventDefault();
      openModal("finishConfirmModal");
    });

    var confirmYes = document.getElementById("finishConfirmYes");
    if (confirmYes) {
      confirmYes.addEventListener("click", function () {
        var actionField = document.getElementById("actionField");
        actionField.disabled = false;
        actionField.value = "finish";
        document.getElementById("examForm").submit();
      });
    }
    var confirmNo = document.getElementById("finishConfirmNo");
    if (confirmNo) {
      confirmNo.addEventListener("click", function () {
        closeModal("finishConfirmModal");
      });
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    startCountdown();
    setupMaxSelectGuard();
    setupFinishConfirm();
  });
})();
