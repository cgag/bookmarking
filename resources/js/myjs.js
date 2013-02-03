$('a.delete-bookmark').click(function(e) {
  // TODO: try doing this with clojurescript and domina
  $.ajax({
    url: $(this).attr('href'),
    type: "POST",
  });
  $(this).parentsUntil('.bookmark-list').remove();
  e.preventDefault();
});
