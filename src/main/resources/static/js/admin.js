/**
 * AquaMate Admin Console Javascript
 */

// ─── 회원 관리 ───

function changeUserRole(userId, selectElement) {
    const newRole = selectElement.value;
    if (!confirm(`'${userId}' 사용자의 권한을 ${newRole}로 변경하시겠습니까?`)) {
        location.reload();
        return;
    }

    $.ajax({
        url: `/aquamate/admin/api/users/${encodeURIComponent(userId)}/role`,
        type: 'POST',
        data: { role: newRole },
        success: function(res) {
            alert(res.message);
            location.reload();
        },
        error: function(xhr) {
            alert(xhr.responseJSON?.message || '권한 변경 실패');
            location.reload();
        }
    });
}

function openResetPwModal(userId) {
    $('#resetUserId').val(userId);
    $('#targetUserDisplay').text(userId);
    $('#newPasswordInput').val('');
    $('#resetPwModal').addClass('active');
}

function submitResetPassword() {
    const userId = $('#resetUserId').val();
    const newPw = $('#newPasswordInput').val();

    if (!newPw || newPw.length < 8) {
        alert('비밀번호는 최소 8자 이상 입력해주세요.');
        return;
    }

    $.ajax({
        url: `/aquamate/admin/api/users/${encodeURIComponent(userId)}/reset-password`,
        type: 'POST',
        data: { newPassword: newPw },
        success: function(res) {
            alert(res.message);
            closeModal('resetPwModal');
        },
        error: function(xhr) {
            alert(xhr.responseJSON?.message || '비밀번호 초기화 실패');
        }
    });
}

function deleteUser(userId) {
    if (!confirm(`'${userId}' 사용자를 정말 강제 탈퇴 처리하시겠습니까?\n이 작업은 되돌릴 수 없으며 북마크 내역도 함께 삭제됩니다.`)) {
        return;
    }

    $.ajax({
        url: `/aquamate/admin/api/users/${encodeURIComponent(userId)}`,
        type: 'DELETE',
        success: function(res) {
            alert(res.message);
            location.reload();
        },
        error: function(xhr) {
            alert(xhr.responseJSON?.message || '회원 삭제 실패');
        }
    });
}

// ─── 물고기 도감 관리 ───

function htmlToLines(html) {
    if (!html) return '';
    if (html.includes('<li>')) {
        const temp = document.createElement('div');
        temp.innerHTML = html;
        const items = Array.from(temp.querySelectorAll('li')).map(li => li.textContent.trim());
        if (items.length > 0) return items.join('\n');
    }
    return html;
}

function previewFishImage(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            $('#imgPreview').attr('src', e.target.result).show();
        };
        reader.readAsDataURL(input.files[0]);
    } else {
        $('#imgPreview').hide().attr('src', '');
    }
}

function openFishModal(fishKey) {
    $('#fishImgFile').val('');
    if (!fishKey) {
        // 신규 등록
        $('#fishModalTitle').text('신규 물고기 등록');
        $('#fishNumBadgeVal').text('조회 중...');
        $('#fishKey').val('').prop('readonly', false);
        $('#fishName').val('');
        $('#fishImg').val('');
        $('#imgPreview').hide().attr('src', '');
        $('#fishBasic').val('');
        $('#fishEnv').val('');
        $('#fishType').val('');
        $('#fishTaming').val('');

        // 다음 도감 번호 자동 조회
        $.ajax({
            url: '/aquamate/admin/api/fish/next-num',
            type: 'GET',
            success: function(res) {
                const nextNum = res.nextNum || '001';
                $('#fishNum').val(nextNum);
                $('#fishNumBadgeVal').text('#' + nextNum);
                $('#fishModal').addClass('active');
            },
            error: function() {
                $('#fishNum').val('001');
                $('#fishNumBadgeVal').text('#001');
                $('#fishModal').addClass('active');
            }
        });
    } else {
        // 기존 수정
        $('#fishModalTitle').text('물고기 정보 수정');
        $('#fishKey').val(fishKey).prop('readonly', true);

        $.ajax({
            url: `/aquamate/admin/api/fish/${encodeURIComponent(fishKey)}`,
            type: 'GET',
            success: function(data) {
                const currentNum = data.num || '--';
                $('#fishNum').val(currentNum);
                $('#fishNumBadgeVal').text('#' + currentNum);
                $('#fishName').val(data.name || '');
                $('#fishImg').val(data.img || '');
                if (data.img) {
                    const imgSrc = data.img.startsWith('/') ? `/aquamate${data.img}` : `/aquamate/${data.img}`;
                    $('#imgPreview').attr('src', imgSrc).show();
                } else {
                    $('#imgPreview').hide();
                }
                $('#fishBasic').val(htmlToLines(data.basic));
                $('#fishEnv').val(htmlToLines(data.env));
                $('#fishType').val(data.type || '');
                $('#fishTaming').val(data.taming || '');
                $('#fishModal').addClass('active');
            },
            error: function() {
                alert('물고기 정보를 불러오지 못했습니다.');
            }
        });
    }
}

function submitFishForm() {
    const key = $('#fishKey').val().trim();
    const name = $('#fishName').val().trim();

    if (!key || !name) {
        alert('고유 키(Key)와 물고기 이름은 필수 항목입니다.');
        return;
    }

    const formData = new FormData();
    formData.append('key', key);
    formData.append('num', $('#fishNum').val().trim());
    formData.append('name', name);
    formData.append('img', $('#fishImg').val().trim());
    formData.append('basic', $('#fishBasic').val());
    formData.append('env', $('#fishEnv').val());
    formData.append('type', $('#fishType').val());
    formData.append('taming', $('#fishTaming').val());

    const fileInput = document.getElementById('fishImgFile');
    if (fileInput && fileInput.files.length > 0) {
        formData.append('imageFile', fileInput.files[0]);
    }

    $.ajax({
        url: '/aquamate/admin/api/fish',
        type: 'POST',
        data: formData,
        contentType: false,
        processData: false,
        success: function(res) {
            alert('저장되었습니다.');
            location.reload();
        },
        error: function(xhr) {
            alert(xhr.responseJSON?.message || '저장 중 오류가 발생했습니다.');
        }
    });
}

function deleteFish(key, name) {
    if (!confirm(`'${name}(${key})' 물고기 도감 데이터를 정말 삭제하시겠습니까?\n관련 북마크도 함께 정리됩니다.`)) {
        return;
    }

    $.ajax({
        url: `/aquamate/admin/api/fish/${encodeURIComponent(key)}`,
        type: 'DELETE',
        success: function(res) {
            alert(res.message);
            location.reload();
        },
        error: function(xhr) {
            alert(xhr.responseJSON?.message || '삭제 실패');
        }
    });
}

function closeModal(modalId) {
    $(`#${modalId}`).removeClass('active');
}
