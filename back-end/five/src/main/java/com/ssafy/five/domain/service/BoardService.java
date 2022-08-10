package com.ssafy.five.domain.service;

import com.ssafy.five.controller.dto.FileDto;
import com.ssafy.five.controller.dto.req.GetUserTypeBoardReqDto;
import com.ssafy.five.controller.dto.req.OnOffBoardLikeReqDto;
import com.ssafy.five.controller.dto.req.RegistBoardReqDto;
import com.ssafy.five.controller.dto.req.UpdateBoardReqDto;
import com.ssafy.five.controller.dto.res.GetBoardResDto;
import com.ssafy.five.domain.entity.Board;
import com.ssafy.five.domain.entity.EnumType.BoardType;
import com.ssafy.five.domain.entity.EnumType.FileType;
import com.ssafy.five.domain.entity.Files;
import com.ssafy.five.domain.entity.LikeBoard;
import com.ssafy.five.domain.entity.Users;
import com.ssafy.five.domain.repository.BoardRepository;
import com.ssafy.five.domain.repository.FileRepository;
import com.ssafy.five.domain.repository.LikeBoardRepository;
import com.ssafy.five.domain.repository.UserRepository;
import com.ssafy.five.exception.BoardNotFoundException;
import com.ssafy.five.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    @Value("${spring.servlet.multipart.location}")
    private String bpath;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final FileRepository fileRepository;
    private final LikeBoardRepository likeBoardRepository;

    @Transactional
    public void regist(RegistBoardReqDto registBoardReqDto, MultipartFile[] multipartFiles) throws Exception {
        // 파일 중 이미지가 있는지 여부
        boolean existImage = false;
        // 파일 중 영상이 있는지 여부
        boolean existVideo = false;

        // 파일 정보 저장 리스트
        List<FileDto> list = new ArrayList<>();

        // 파일을 서버 로컬에 저장
        if (multipartFiles != null) {
            for (MultipartFile file : multipartFiles) {
                if (!file.isEmpty()) {
                    String newFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    String uploadpath;
                    FileType fileType;

                    // 파일 중 비디오 타입이 있다면
                    if (file.getContentType().split("/")[0].equals("video")) {
                        existVideo = true;
                        uploadpath = "VIDEO/";

                        fileType = FileType.VIDEO;
                    }
                    // 파일 중 이미지 타입이 있다면
                    else if (file.getContentType().split("/")[0].equals("image")) {
                        existImage = true;
                        uploadpath = "IMAGE/";

                        fileType = FileType.IMAGE;
                    } else {
                        uploadpath = "GENERAL/";

                        fileType = FileType.GENERAL;
                    }

                    list.add(new FileDto(newFileName, fileType, (int) file.getSize()));

                    // UUID+파일원본이름을 가진 새로운 파일 객체를 생성하여 로컬에 저장
                    File newFile = new File(uploadpath, newFileName);
                    file.transferTo(newFile);
                }
            }
        }

        // 파일 중 영상 파일이 있다면
        Board boardEntity;
        if (existVideo) {
            boardEntity = boardRepository.save(registBoardReqDto.toEntity(BoardType.VIDEO));
        }
        // 영상 파일은 없고 이미지 파일은 있다면
        else if (existImage) {
            boardEntity = boardRepository.save(registBoardReqDto.toEntity(BoardType.IMAGE));
        }
        // 영상, 이미지 파일이 없다면
        else {
            boardEntity = boardRepository.save(registBoardReqDto.toEntity(BoardType.GENERAL));
        }

        // db에 파일 정보 저장
        for (FileDto item : list) {
            fileRepository.save(Files.builder()
                    .board(boardEntity)
                    .fileName(item.getFileName())
                    .fileType(item.getFileType())
                    .fileSize(item.getFileSize())
                    .build());
        }
    }

    public List<GetBoardResDto> findAll() {
        List<Board> list = boardRepository.findAll();
        return list.stream().map(GetBoardResDto::new).collect(Collectors.toList());
    }

    @Transactional
    public boolean update(UpdateBoardReqDto updateBoardReqDto) {
        int result = boardRepository.updateBoard(updateBoardReqDto.getBoardTitle(),
                updateBoardReqDto.getBoardContent(),
                updateBoardReqDto.getBoardId(),
                new Date());
        return (result > 0 ? true : false);
    }

    @Transactional
    public void deleteById(Long boardId) {
        Board boardEntity = boardRepository.findById(boardId).orElseThrow(() -> new BoardNotFoundException());
        List<Files> files = fileRepository.findAllByBoard(boardEntity);
        // DB에 있는 게시글, 댓글, 파일 정보 삭제
        boardRepository.deleteById(boardId);

        // 서버 로컬에 저장된 파일 삭제
        for (Files file : files) {
            File dfile = new File(bpath + "/" + file.getFileType() + "/" + file.getFileName());
            if (dfile.exists()) {
                dfile.delete();
            }
        }
    }

    @Transactional
    public GetBoardResDto findById(Long boardId) {
        boardRepository.updateHit(boardId);
        Board entity = boardRepository.findById(boardId).orElseThrow(() -> new BoardNotFoundException());
        return new GetBoardResDto(entity);
    }

    public List<GetBoardResDto> findAllByBoardType(BoardType boardType) {
        List<Board> boards = boardRepository.findAllByBoardType(boardType);
        return boards.stream().map(GetBoardResDto::new).collect(Collectors.toList());
    }

    @Transactional
    public void onOffBoardLike(OnOffBoardLikeReqDto onOffBoardLikeReqDto) {
        Users users = userRepository.findById(onOffBoardLikeReqDto.getUserId()).orElseThrow(() -> new UserNotFoundException());
        Board board = boardRepository.findById(onOffBoardLikeReqDto.getBoardId()).orElseThrow(() -> new BoardNotFoundException());
        LikeBoard likeBoard = likeBoardRepository.findByUserAndBoard(users, board);

        // 해당 유저가 해당 게시글을 좋아요를 한 상태라면 OFF
        if (likeBoard != null) {
            likeBoardRepository.delete(likeBoard);

            // 게시글 좋아요 수 감소
            boardRepository.save(Board.builder()
                    .user(users)
                    .boardId(board.getBoardId())
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .boardType(board.getBoardType())
                    .boardHit(board.getBoardHit())
                    .boardLikes(board.getBoardLikes() - 1)
                    .boardDate(board.getBoardDate())
                    .boardUpdate(board.getBoardUpdate())
                    .build());
        }
        // 해당 유저가 해당 게시글을 좋아요를 안한 상태라면 ON
        else {
            likeBoard = LikeBoard.builder()
                    .users(users)
                    .board(board)
                    .build();
            likeBoardRepository.save(likeBoard);

            // 게시글 좋아요 수 증가
            boardRepository.save(Board.builder()
                    .user(users)
                    .boardId(board.getBoardId())
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .boardType(board.getBoardType())
                    .boardHit(board.getBoardHit())
                    .boardLikes(board.getBoardLikes() + 1)
                    .boardDate(board.getBoardDate())
                    .boardUpdate(board.getBoardUpdate())
                    .build());
        }
    }

    public List<GetBoardResDto> findAllByUser(String userId) {
        Users usersEntity = userRepository.findByUserId(userId);
        List<Board> likeBoards = likeBoardRepository.findAllByUser(usersEntity);
        return likeBoards.stream().map(GetBoardResDto::new).collect(Collectors.toList());
    }

    public List<GetBoardResDto> findAllByUserAndType(GetUserTypeBoardReqDto getUserTypeBoardReqDto) {
        Users userEntity = userRepository.findByUserId(getUserTypeBoardReqDto.getUserId());
        return boardRepository.findAllByUserAndType(userEntity, getUserTypeBoardReqDto.getBoardType());
    }

    public List<GetBoardResDto> findRandomVideo() {
        List<Board> boards = boardRepository.findRandomVideo();
        return boards.stream().map(GetBoardResDto::new).collect(Collectors.toList());
    }

    public List<GetBoardResDto> findAllByUserLatest(String userId, String time) {
        Users userEntity = userRepository.findByUserId(userId);

        List<Board> boards = null;
        if (time.equals("NEW")) {
            boards = boardRepository.findAllByUserNEW(userEntity);
        } else if (time.equals("OLD")) {
            boards = boardRepository.findAllByUserOLD(userEntity);
        }
        return boards.stream().map(GetBoardResDto::new).collect(Collectors.toList());
    }
}