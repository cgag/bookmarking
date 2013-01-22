$('a.delete-bookmark').click(function(e) {
  // TODO: maybe put a spinning clock or something on the screen
  // until we get a confirmation?
  // TODO: try doing this with clojurescript and domina
  // TODO: categories get deleted when they have no links, shoudl they remain?  Would require separate categories table
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
