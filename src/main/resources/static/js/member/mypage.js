$(function () {
    console.log("zzz");
    $('#nicknameButton').click(function () {
        let data = $(this).val();
        if (data == "true") {
            $('#nicknameButton').val(false);
            $('#nicknameButton').text("비공개");
        } else {
            $('#nicknameButton').val(true);
            $('#nicknameButton').text("공개");
        }
    });

    $('#out').click(function () {
        if (confirm("정말 탈퇴 하시겠습니까?")) {
            $.ajax({
                type: 'get',
                url: '/memberDelete',
                dataType: 'json',
                success: function (result) {
                    location.href = "/";
                }
            });
        } else {

        }
    });

    $('#memberChangeButton').click(function (){
        let data = {
            nickname : $('#newnickname').val(),
            baseAddress : $('#address').val(),
            postcode : $('#postcode').val(),
            detailAddress : $('#detailAddress').val(),
        };

        if (data.nickname==''){
            alert("닉네임을 입력해주세요");
            return false;
        }
       $.ajax({
           type: 'post',
           url : '/mypage/change',
           dataType: 'json',
           contentType: 'application/json; charset=utf-8',
           data: JSON.stringify(data)
       }).done(function (result){
           console.log(result.nicknameResult);
           console.log(result.addressResult);
           if (result.nicknameResult == 'noChange' && result.addressResult=='noChange') {
               alert("변경할 정보가 없습니다.");
           }else if (result.nicknameResult == 'overlap'){
               alert("중복된 닉네임 입니다.");
           }else if (result.nicknameResult == 'ok' || result.addressResult == 'ok') {
               alert("변경 완료 되었습니다.");
           }

       }).fail(function (error){
           alert(JSON.stringify(error))
       })

    })
});