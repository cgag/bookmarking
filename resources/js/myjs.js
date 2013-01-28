$('a.delete-bookmark').click(function(e) {
  // TODO: try doing this with clojurescript and domina
  $.ajax({
    url: $(this).attr('href'),
    type: "POST",
    success: function(data, txtStatus, xhr) {
      console.log(data);
    }
  });
  $(this).parentsUntil('.bookmark-list').remove();
  e.preventDefault();
});
