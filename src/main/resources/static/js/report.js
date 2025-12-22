// report.js – UX only (no business logic)

document.addEventListener("DOMContentLoaded", function () {

  // Disable submit if dates invalid
  const form = document.querySelector("form");
  if (!form) return;

  form.addEventListener("submit", function (e) {
    const start = form.querySelector('input[name="startDate"]').value;
    const end = form.querySelector('input[name="endDate"]').value;

    if (start && end && start > end) {
      e.preventDefault();
      alert("Start Date tidak boleh lebih besar dari End Date");
    }
  });

  // Small click animation for buttons
  document.querySelectorAll("button, .btn-back").forEach(btn => {
    btn.addEventListener("mousedown", () => {
      btn.style.transform = "scale(0.97)";
    });
    btn.addEventListener("mouseup", () => {
      btn.style.transform = "";
    });
    btn.addEventListener("mouseleave", () => {
      btn.style.transform = "";
    });
  });

});
// Date preset handler (UX only)
document.addEventListener("DOMContentLoaded", function () {

  const preset = document.getElementById("presetRange");
  const start = document.getElementById("startDate");
  const end = document.getElementById("endDate");

  if (!preset || !start || !end) return;

  preset.addEventListener("change", function () {
    const today = new Date();
    let startDate, endDate;

    if (this.value === "today") {
      startDate = endDate = today;
    } else if (this.value === "this_month") {
      startDate = new Date(today.getFullYear(), today.getMonth(), 1);
      endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    } else if (this.value === "this_year") {
      startDate = new Date(today.getFullYear(), 0, 1);
      endDate = new Date(today.getFullYear(), 11, 31);
    } else {
      return; // custom
    }

    start.value = startDate.toISOString().split("T")[0];
    end.value = endDate.toISOString().split("T")[0];
  });

});
